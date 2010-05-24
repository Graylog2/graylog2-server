require 'socket'
require 'net/https' 
require 'net/http'
require 'logger'
require 'zlib'
require 'stringio'

# The NewRelic Agent collects performance data from ruby applications
# in realtime as the application runs, and periodically sends that
# data to the NewRelic server.
module NewRelic
  module Agent
  
  # The Agent is a singleton that is instantiated when the plugin is
  # activated.
  class Agent
    
    # Specifies the version of the agent's communication protocol with
    # the NewRelic hosted site.
    
    PROTOCOL_VERSION = 8
    # 14105: v8 (tag 2.10.3)
    # (no v7)
    # 10379: v6 (not tagged)
    # 4078:  v5 (tag 2.5.4)
    # 2292:  v4 (tag 2.3.6)
    # 1754:  v3 (tag 2.3.0)
    # 534:   v2 (shows up in 2.1.0, our first tag)
    
    
    attr_reader :obfuscator
    attr_reader :stats_engine
    attr_reader :transaction_sampler
    attr_reader :error_collector
    attr_reader :record_sql
    attr_reader :histogram
    attr_reader :metric_ids
    
    # Should only be called by NewRelic::Control
    def self.instance
      @instance ||= self.new
    end
    # This method is deprecated.  Use NewRelic::Agent.manual_start
    def manual_start(ignored=nil, also_ignored=nil)
      raise "This method no longer supported.  Instead use the class method NewRelic::Agent.manual_start"
    end
    
    # This method should be called in a forked process after a fork.
    # It assumes the parent process initialized the agent, but does
    # not assume the agent started.  
    #
    # * It clears any metrics carried over from the parent process
    # * Restarts the sampler thread if necessary
    # * Initiates a new agent run and worker loop unless that was done
    #   in the parent process and +:force_reconnect+ is not true
    # 
    # Options:
    # * <tt>:force_reconnect => true</tt> to force the spawned process to 
    #   establish a new connection, such as when forking a long running process.
    #   The default is false--it will only connect to the server if the parent
    #   had not connected.
    # * <tt>:keep_retrying => false</tt> if we try to initiate a new 
    #   connection, this tells me to only try it once so this method returns
    #   quickly if there is some kind of latency with the server.
    def after_fork(options={})
      
      # @connected gets false after we fail to connect or have an error
      # connecting.  @connected has nil if we haven't finished trying to connect.
      # or we didn't attempt a connection because this is the master process

      log.debug "Agent received after_fork notice in #$$: [#{control.agent_enabled?}; monitor=#{control.monitor_mode?}; connected: #{@connected.inspect}; thread=#{@worker_thread.inspect}]"
      return if !control.agent_enabled? or
                !control.monitor_mode? or
                @connected == false or
                @worker_thread && @worker_thread.alive?

      log.debug "Detected that the worker thread is not running in #$$.  Restarting."

      # Clear out stats that are left over from parent process
      reset_stats
      
      # Don't ever check to see if this is a spawner.  If we're in a forked process
      # I'm pretty sure we're not also forking new instances.
      start_worker_thread(options.merge(:check_for_spawner => false))
      @stats_engine.start_sampler_thread
    end
    
    # True if we have initialized and completed 'start'
    def started?
      @started
    end
    
    # Attempt a graceful shutdown of the agent.  
    def shutdown
      return if not started?
      if @worker_loop
        @worker_loop.stop
        
        log.debug "Starting Agent shutdown"
        
        # if litespeed, then ignore all future SIGUSR1 - it's
        # litespeed trying to shut us down
        
        if control.dispatcher == :litespeed
          Signal.trap("SIGUSR1", "IGNORE")
          Signal.trap("SIGTERM", "IGNORE")
        end
        
        begin
          graceful_disconnect
        rescue => e
          log.error e
          log.error e.backtrace.join("\n")
        end
      end
      @started = nil
    end
    
    def start_transaction
      @stats_engine.start_transaction
    end
    
    def end_transaction
      @stats_engine.end_transaction
    end
    
    def set_record_sql(should_record)
      prev = Thread::current[:record_sql]
      Thread::current[:record_sql] = should_record
      prev.nil? || prev
    end
    
    def set_record_tt(should_record)
      prev = Thread::current[:record_tt]
      Thread::current[:record_tt] = should_record
      prev.nil? || prev
    end
    # Push flag indicating whether we should be tracing in this
    # thread.  
    def push_trace_execution_flag(should_trace=false)
      (Thread.current[:newrelic_untraced] ||= []) << should_trace 
    end

    # Pop the current trace execution status.  Restore trace execution status
    # to what it was before we pushed the current flag.
    def pop_trace_execution_flag
      Thread.current[:newrelic_untraced].pop if Thread.current[:newrelic_untraced]
    end
    
    def set_sql_obfuscator(type, &block)
      if type == :before
        @obfuscator = NewRelic::ChainedCall.new(block, @obfuscator)
      elsif type == :after
        @obfuscator = NewRelic::ChainedCall.new(@obfuscator, block)
      elsif type == :replace
        @obfuscator = block
      else
        fail "unknown sql_obfuscator type #{type}"
      end
    end

    def log
      NewRelic::Agent.logger
    end  
    
    # Start up the agent.  This verifies that the agent_enabled? is
    # true and initializes the sampler based on the current
    # controluration settings.  Then it will fire up the background
    # thread for sending data to the server if applicable.
    def start
      if started?
        control.log! "Agent Started Already!", :error
        return
      end
      return if !control.agent_enabled? 
      @started = true
      @local_host = determine_host
      
      if control.dispatcher.nil? || control.dispatcher.to_s.empty?
        log.info "No dispatcher detected."
      else
        log.info "Dispatcher: #{control.dispatcher.to_s}"
      end
      log.info "Application: #{control.app_names.join(", ")}" unless control.app_names.empty?
      
      sampler_config = control.fetch('transaction_tracer', {})
      @should_send_samples = sampler_config.fetch('enabled', true)
      log.info "Transaction tracing not enabled." if not @should_send_samples
      
      @record_sql = sampler_config.fetch('record_sql', :obfuscated).to_sym
      
      # use transaction_threshold: 4.0 to force the TT collection
      # threshold to 4 seconds
      # use transaction_threshold: apdex_f to use your apdex t value
      # multiplied by 4
      # undefined transaction_threshold defaults to 2.0
      apdex_f = 4 * NewRelic::Control.instance.apdex_t
      @slowest_transaction_threshold = sampler_config.fetch('transaction_threshold', 2.0)
      if @slowest_transaction_threshold =~ /apdex_f/i
        @slowest_transaction_threshold = apdex_f
      end
      @slowest_transaction_threshold = @slowest_transaction_threshold.to_f
      
      @explain_threshold = sampler_config.fetch('explain_threshold', 0.5).to_f
      @explain_enabled = sampler_config.fetch('explain_enabled', true)
      @random_sample = sampler_config.fetch('random_sample', false)
      log.warn "Agent is configured to send raw SQL to RPM service" if @record_sql == :raw
      # Initialize transaction sampler
      @transaction_sampler.random_sampling = @random_sample

      if control.monitor_mode?
        if !control.license_key
          control.log! "No license key found.  Please edit your newrelic.yml file and insert your license key.", :error
        elsif  control.license_key.length != 40
          control.log! "Invalid license key: #{control.license_key}", :error
        else     
          # Do the connect in the foreground if we are in sync mode
          NewRelic::Agent.disable_all_tracing { connect(:keep_retrying => false) } if control.sync_startup
          
          # Start the event loop and initiate connection if necessary
          start_worker_thread

          # Our shutdown handler needs to run after other shutdown handlers
          # that may be doing things like running the app (hello sinatra).  
          if RUBY_VERSION =~ /rubinius/i 
            list = at_exit { shutdown }
            # move the shutdown handler to the front of the list, to
            # execute last:
            list.unshift(list.pop)
          elsif !defined?(JRuby) or !defined?(Sinatra::Application)
            at_exit { at_exit { shutdown } } 
          end
        end
      end
      control.log! "New Relic RPM Agent #{NewRelic::VERSION::STRING} Initialized: pid = #$$"
      control.log! "Agent Log found in #{NewRelic::Control.instance.log_file}" if NewRelic::Control.instance.log_file
    end
    
    # Clear out the metric data, errors, and transaction traces.  Reset the histogram data.
    def reset_stats
      @stats_engine.reset_stats
      @unsent_errors = []
      @traces = nil
      @unsent_timeslice_data = {}
      @last_harvest_time = Time.now
      @launch_time = Time.now
      @histogram = NewRelic::Histogram.new(NewRelic::Control.instance.apdex_t / 10)
    end

    private
    def collector
      @collector ||= control.server
    end
    
    # Try to launch the worker thread and connect to the server.
    # 
    # See #connect for a description of connection_options.
    def start_worker_thread(connection_options = {})
      log.debug "Creating RPM worker thread."
      @worker_thread = Thread.new do
        begin
          NewRelic::Agent.disable_all_tracing do
            # We try to connect.  If this returns false that means
            # the server rejected us for a licensing reason and we should 
            # just exit the thread.  If it returns nil
            # that means it didn't try to connect because we're in the master.
            connect(connection_options)
            if @connected
              # disable transaction sampling if disabled by the server and we're not in dev mode
              if !control.developer_mode? && !@should_send_samples
                @transaction_sampler.disable
              end
              log.info "Reporting performance data every #{@report_period} seconds."
              log.debug "Running worker loop"
              # note if the agent attempts to report more frequently than allowed by the server
              # the server will start dropping data.
              @worker_loop = WorkerLoop.new
              @worker_loop.run(@report_period) do
                harvest_and_send_timeslice_data
                harvest_and_send_slowest_sample if @should_send_samples
                harvest_and_send_errors if error_collector.enabled
              end
            else
              log.debug "No connection.  Worker thread finished."
            end
          end
        rescue NewRelic::Agent::ForceRestartException => e
          log.info e.message
          # disconnect and start over.
          # clear the stats engine
          reset_stats
          @metric_ids = {}
          @connected = nil
          # Wait a short time before trying to reconnect
          sleep 30
          retry
        rescue NewRelic::Agent::ForceDisconnectException => e
          # when a disconnect is requested, stop the current thread, which
          # is the worker thread that gathers data and talks to the
          # server.
          log.error "RPM forced this agent to disconnect (#{e.message})"
          @connected = false
        rescue NewRelic::Agent::ServerConnectionException => e
          control.log! "Unable to establish connection with the server.  Run with log level set to debug for more information."
          log.debug("#{e.class.name}: #{e.message}\n#{e.backtrace.first}")
          @connected = false
        rescue Exception => e
          log.error "Terminating worker loop: #{e.class.name}: #{e}\n  #{e.backtrace.join("\n  ")}"
          @connected = false
        end # begin
      end # thread new
      @worker_thread['newrelic_label'] = 'Worker Loop'
    end
    
    def control
      NewRelic::Control.instance
    end
    
    def initialize
      @launch_time = Time.now
      
      @metric_ids = {}
      @histogram = NewRelic::Histogram.new(NewRelic::Control.instance.apdex_t / 10)
      @stats_engine = NewRelic::Agent::StatsEngine.new
      @transaction_sampler = NewRelic::Agent::TransactionSampler.new
      @stats_engine.transaction_sampler = @transaction_sampler
      @error_collector = NewRelic::Agent::ErrorCollector.new
      
      @request_timeout = NewRelic::Control.instance.fetch('timeout', 2 * 60)
      
      @last_harvest_time = Time.now
      @obfuscator = method(:default_sql_obfuscator)
    end
    
    # Connect to the server and validate the license.  If successful,
    # @connected has true when finished.  If not successful, you can
    # keep calling this.  Return false if we could not establish a
    # connection with the server and we should not retry, such as if
    # there's a bad license key.
    #
    # Set keep_retrying=false to disable retrying and return asap, such as when
    # invoked in the foreground.  Otherwise this runs until a successful
    # connection is made, or the server rejects us.
    #
    # * <tt>:keep_retrying => false</tt> to only try to connect once, and
    #   return with the connection set to nil.  This ensures we may try again
    #   later (default true).
    # * <tt>force_reconnect => true</tt> if you want to establish a new connection
    #   to the server before running the worker loop.  This means you get a separate
    #   agent run and RPM sees it as a separate instance (default is false).  
    # * <tt>:check_for_spawner => false</tt> to omit the check to see if we are
    #   an application spawner.  We detect the spawner and stop the agent so we don't
    #   report stats from a spawner.  You don't want to do this check if you _know_
    #   you are not in a spawner (default is true).
    
    def connect(options)
      return if @connected && !options[:force_reconnect]      
      keep_retrying = options[:keep_retrying].nil? || options[:keep_retrying]
      check_for_spawner = options[:check_for_spawner].nil? || options[:check_for_spawner]
      
      # wait a few seconds for the web server to boot, necessary in development
      connect_retry_period = keep_retrying ? 10 : 0
      connect_attempts = 0
      @agent_id = nil
      begin
        sleep connect_retry_period.to_i
        # Running in the Passenger or Unicorn spawners?
        if check_for_spawner && $0 =~ /ApplicationSpawner|^unicorn\S* master/
          log.debug "Process is master spawner (#$0) -- don't connect to RPM service"
          @connected = nil
          return 
        else 
          log.debug "Connecting Process to RPM: #$0"
        end
        environment = control['send_environment_info'] != false ? control.local_env.snapshot : []
        log.debug "Connecting with validation seed/token: #{control.validate_seed}/#{control.validate_token}" if control.validate_seed
        @agent_id ||= invoke_remote :start, @local_host, {
          :pid => $$, 
          :launch_time => @launch_time.to_f, 
          :agent_version => NewRelic::VERSION::STRING, 
          :environment => environment,
          :settings => control.settings,
          :validate_seed => control.validate_seed,
          :validate_token => control.validate_token }
        
        host = invoke_remote(:get_redirect_host)
        
        @collector = control.server_from_host(host) if host        
            
        @report_period = invoke_remote :get_data_report_period, @agent_id
 
        control.log! "Connected to NewRelic Service at #{@collector}"
        log.debug "Agent ID = #{@agent_id}."
        
        # Ask the server for permission to send transaction samples.
        # determined by subscription license.
        @should_send_samples &&= invoke_remote :should_collect_samples, @agent_id
        
        if @should_send_samples
          sampling_rate = invoke_remote :sampling_rate, @agent_id if @random_sample
          @transaction_sampler.sampling_rate = sampling_rate
          log.info "Transaction sample rate: #{@transaction_sampler.sampling_rate}" if sampling_rate
          log.info "Transaction tracing threshold is #{@slowest_transaction_threshold} seconds." 
        end
        
        # Ask for permission to collect error data
        error_collector.enabled &&= invoke_remote(:should_collect_errors, @agent_id)
        
        log.info "Transaction traces will be sent to the RPM service." if @should_send_samples
        log.info "Errors will be sent to the RPM service." if error_collector.enabled
        
        @connected_pid = $$
        @connected = true
        
      rescue NewRelic::Agent::LicenseException => e
        control.log! e.message, :error
        control.log! "Visit NewRelic.com to obtain a valid license key, or to upgrade your account."
        @connected = false
        
      rescue Timeout::Error, StandardError => e
        log.info "Unable to establish connection with New Relic RPM Service at #{control.server}"
        unless e.instance_of? NewRelic::Agent::ServerConnectionException
          log.error e.message
          log.debug e.backtrace.join("\n")
        end
        # retry logic
        if keep_retrying
          connect_attempts += 1
          case connect_attempts
          when 1..2
            connect_retry_period, period_msg = 60, "1 minute"
          when 3..5 
            connect_retry_period, period_msg = 60 * 2, "2 minutes"
          else 
            connect_retry_period, period_msg = 5 * 60, "5 minutes"
          end
          log.info "Will re-attempt in #{period_msg}" 
          retry
        else
          @connected = nil
        end
      end
    end
      
    def determine_host
      Socket.gethostname
    end
    
    def determine_home_directory
      control.root
    end
    
    def harvest_and_send_timeslice_data
      
      NewRelic::Agent::BusyCalculator.harvest_busy
      
      now = Time.now
            
      @unsent_timeslice_data ||= {}
      @unsent_timeslice_data = @stats_engine.harvest_timeslice_data(@unsent_timeslice_data, @metric_ids)
      
      begin
        # In this version of the protocol, we get back an assoc array of spec to id.
        metric_ids = invoke_remote(:metric_data, @agent_id, 
                                   @last_harvest_time.to_f, 
                                   now.to_f, 
                                   @unsent_timeslice_data.values)
        
      rescue Timeout::Error
        # assume that the data was received. chances are that it was
        metric_ids = nil
      end
      
      metric_ids.each do | spec, id |
        @metric_ids[spec] = id
      end if metric_ids 
      
      log.debug "#{now}: sent #{@unsent_timeslice_data.length} timeslices (#{@agent_id}) in #{Time.now - now} seconds"
      
      # if we successfully invoked this web service, then clear the unsent message cache.
      @unsent_timeslice_data = {}
      @last_harvest_time = now
      
      # handle_messages 
      
      # note - exceptions are logged in invoke_remote.  If an exception is encountered here,
      # then the metric data is downsampled for another timeslices
    end
    
    def harvest_and_send_slowest_sample
      @traces = @transaction_sampler.harvest(@traces, @slowest_transaction_threshold)
      
      unless @traces.empty?
        now = Time.now
        log.debug "Sending (#{@traces.length}) transaction traces"
        begin        
          # take the traces and prepare them for sending across the
          # wire.  This includes gathering SQL explanations, stripping
          # out stack traces, and normalizing SQL.  note that we
          # explain only the sql statements whose segments' execution
          # times exceed our threshold (to avoid unnecessary overhead
          # of running explains on fast queries.)
          traces = @traces.collect {|trace| trace.prepare_to_send(:explain_sql => @explain_threshold, :record_sql => @record_sql, :keep_backtraces => true, :explain_enabled => @explain_enabled)} 
          invoke_remote :transaction_sample_data, @agent_id, traces
        rescue PostTooBigException
          # we tried to send too much data, drop the first trace and
          # try again
          retry if @traces.shift
        end
        
        log.debug "Sent slowest sample (#{@agent_id}) in #{Time.now - now} seconds"
      end
      
      # if we succeed sending this sample, then we don't need to keep
      # the slowest sample around - it has been sent already and we
      # can collect the next one
      @traces = nil
      
      # note - exceptions are logged in invoke_remote.  If an
      # exception is encountered here, then the slowest sample of is
      # determined of the entire period since the last reported
      # sample.
    end
    
    def harvest_and_send_errors
      @unsent_errors = @error_collector.harvest_errors(@unsent_errors)
      if @unsent_errors && @unsent_errors.length > 0
        log.debug "Sending #{@unsent_errors.length} errors"
        begin        
          invoke_remote :error_data, @agent_id, @unsent_errors
        rescue PostTooBigException
          @unsent_errors.shift
          retry
        end
        # if the remote invocation fails, then we never clear
        # @unsent_errors, and therefore we can re-attempt to send on
        # the next heartbeat.  Note the error collector maxes out at
        # 20 instances to prevent leakage
        @unsent_errors = []
      end
    end

    def compress_data(object)
      dump = Marshal.dump(object)
      
      # this checks to make sure mongrel won't choke on big uploads
      check_post_size(dump)
      
      # we currently optimize for CPU here since we get roughly a 10x
      # reduction in message size with this, and CPU overhead is at a
      # premium. For extra-large posts, we use the higher compression
      # since otherwise it actually errors out.
      
      dump_size = dump.size
      
      # small payloads don't need compression      
      return [dump, 'identity'] if dump_size < 2000
      
      # medium payloads get fast compression, to save CPU
      # big payloads get all the compression possible, to stay under
      # the 2,000,000 byte post threshold      
      compression = dump_size < 2000000 ? Zlib::BEST_SPEED : Zlib::BEST_COMPRESSION
      
      [Zlib::Deflate.deflate(dump, compression), 'deflate']
    end
    
    def check_post_size(post_string)
      # TODO: define this as a config option on the server side
      return if post_string.size < control.post_size_limit
      log.warn "Tried to send too much data: #{post_string.size} bytes"
      raise PostTooBigException
    end

    def send_request(opts)
      request = Net::HTTP::Post.new(opts[:uri], 'CONTENT-ENCODING' => opts[:encoding], 'ACCEPT-ENCODING' => 'gzip', 'HOST' => opts[:collector].name)
      request.content_type = "application/octet-stream"
      request.body = opts[:data]
      
      log.debug "Connect to #{opts[:collector]}#{opts[:uri]}"
      
      response = nil
      http = control.http_connection(collector)      
      begin
        timeout(@request_timeout) do      
          response = http.request(request)
        end
      rescue Timeout::Error
        log.warn "Timed out trying to post data to RPM (timeout = #{@request_timeout} seconds)" unless @request_timeout < 30
        raise
      end
      if response.is_a? Net::HTTPServiceUnavailable
        raise NewRelic::Agent::ServerConnectionException, "Service unavailable: #{response.body || response.message}"
      elsif response.is_a? Net::HTTPGatewayTimeOut
        log.debug("Timed out getting response: #{response.message}")
        raise Timeout::Error, response.message
      elsif !(response.is_a? Net::HTTPSuccess)
        raise NewRelic::Agent::ServerConnectionException, "Unexpected response from server: #{response.code}: #{response.message}" 
      end
      response
    end

    def decompress_response(response)
      if response['content-encoding'] != 'gzip'
        log.debug "Uncompressed content returned"
        return response.body
      end
      log.debug "Decompressing return value"
      i = Zlib::GzipReader.new(StringIO.new(response.body))
      i.read
    end

    def check_for_exception(response)
      dump = decompress_response(response)
      value = Marshal.load(dump)
      raise value if value.is_a? Exception
      value
    end
    
    def remote_method_uri(method)
      uri = "/agent_listener/#{PROTOCOL_VERSION}/#{control.license_key}/#{method}"
      uri << "?run_id=#{@agent_id}" if @agent_id
      uri
    end
      
    # send a message via post
    def invoke_remote(method, *args)
      #determines whether to zip the data or send plain
      post_data, encoding = compress_data(args)
      
      response = send_request({:uri => remote_method_uri(method), :encoding => encoding, :collector => collector, :data => post_data})

      # raises the right exception if the remote server tells it to die
      return check_for_exception(response)
    rescue NewRelic::Agent::ForceRestartException => e
      log.info e.message
      raise
    rescue SystemCallError, SocketError => e
      # These include Errno connection errors 
      raise NewRelic::Agent::ServerConnectionException, "Recoverable error connecting to the server: #{e}"
    end
    
    def graceful_disconnect
      if @connected
        begin
          log.debug "Sending graceful shutdown message to #{control.server}"
          
          @request_timeout = 10
          log.debug "Flushing unsent metric data to server"
          @worker_loop.run_task
          if @connected_pid == $$
            log.debug "Sending RPM service agent run shutdown message"
            invoke_remote :shutdown, @agent_id, Time.now.to_f
          else
            log.debug "This agent connected from #{@connected_pid}--not sending shutdown"
          end
          log.debug "Graceful disconnect complete"
        rescue Timeout::Error, StandardError 
        end
      else
        log.debug "Bypassing graceful disconnect - agent not connected"
      end
    end
    def default_sql_obfuscator(sql)
      sql = sql.dup
      # This is hardly readable.  Use the unit tests.
      # remove single quoted strings:
      sql.gsub!(/'(.*?[^\\'])??'(?!')/, '?')
      # remove double quoted strings:
      sql.gsub!(/"(.*?[^\\"])??"(?!")/, '?')
      # replace all number literals
      sql.gsub!(/\d+/, "?")
      sql
    end
  end
  
end
end
