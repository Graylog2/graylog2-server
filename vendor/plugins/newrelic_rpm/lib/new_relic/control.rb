require 'yaml'
require 'new_relic/local_environment'
require 'singleton'
require 'erb'
require 'socket'
require 'net/https'
require 'logger'

module NewRelic 
  
  # The Control is a singleton responsible for the startup and
  # initialization sequence.  The initializer uses a LocalEnvironment to 
  # detect the framework and instantiates the framework specific
  # subclass.
  #
  # The Control also implements some of the public API for the agent.
  # 
  class Control
    
    # A flag used in dev mode to indicate if profiling is available
    def profiling?
      @profiling
    end
    
    def profiling_available?
      @profiling_available ||= 
      begin
        require 'ruby-prof'
        true
      rescue LoadError; end
    end
    # Set the flag for capturing profiles in dev mode.  If RubyProf is not
    # loaded a true value is ignored.
    def profiling=(val)
      @profiling = profiling_available? && val && defined?(RubyProf)
    end
    
    attr_accessor :log_file
    # The env is the setting used to identify which section of the newrelic.yml
    # to load.  This defaults to a framework specific value, such as ENV['RAILS_ENV']
    # but can be overridden as long as you set it before calling #init_plugin
    attr_writer :env
    attr_reader :local_env
    
    # Structs holding info for the remote server and proxy server
    class Server < Struct.new :name, :port, :ip #:nodoc:
      def to_s; "#{name}:#{port}"; end
    end
    
    ProxyServer = Struct.new :name, :port, :user, :password #:nodoc:
    
    # Access the Control singleton, lazy initialized
    def self.instance
      @instance ||= new_instance
    end
    
    # Initialize the plugin/gem and start the agent.  This does the necessary configuration based on the
    # framework environment and determines whether or not to start the agent.  If the
    # agent is not going to be started then it loads the agent shim which has stubs
    # for all the external api.
    #
    # This may be invoked multiple times, as long as you don't attempt to uninstall
    # the agent after it has been started.
    #
    # If the plugin is initialized and it determines that the agent is not enabled, it 
    # will skip starting it and install the shim.  But if you later call this with 
    # <tt>:agent_enabled => true</tt>, then it will install the real agent and start it.
    #
    # What determines whether the agent is launched is the result of calling agent_enabled?
    # This will indicate whether the instrumentation should/will be installed.  If we're
    # in a mode where tracers are not installed then we should not start the agent.
    #
    # Subclasses are not allowed to override, but must implement init_config({}) which
    # is called one or more times.
    #
    def init_plugin(options={})
      options['app_name'] = ENV['NEWRELIC_APP_NAME'] if ENV['NEWRELIC_APP_NAME']
 
      require 'new_relic/agent'
      
      # Load the DJ injection now.  If you do it sooner, DJ might not be loaded and
      # you'll miss it.
      require 'new_relic/delayed_job_injection'
      
      # Merge the stringified options into the config as overrides:
      logger_override = options.delete(:log)
      environment_name = options.delete(:env) and self.env = environment_name
      dispatcher = options.delete(:dispatcher) and @local_env.dispatcher = dispatcher 
      dispatcher_instance_id = options.delete(:dispatcher_instance_id) and @local_env.dispatcher_instance_id = dispatcher_instance_id

      # Clear out the settings, if they've already been loaded.  It may be that
      # between calling init_plugin the first time and the second time, the env
      # has been overridden
      @settings = nil
      settings
      merge_options(options)
      if logger_override
        @log = logger_override
        # Try to grab the log filename
        @log_file = @log.instance_eval { @logdev.filename rescue nil }
      end
      # An artifact of earlier implementation, we put both #add_method_tracer and #trace_execution
      # methods in the module methods.
      Module.send :include, NewRelic::Agent::MethodTracer::ClassMethods
      Module.send :include, NewRelic::Agent::MethodTracer::InstanceMethods
      init_config(options)
      NewRelic::Agent.agent = NewRelic::Agent::Agent.instance
      if agent_enabled? && !NewRelic::Agent.instance.started?
        setup_log unless logger_override
        start_agent
        install_instrumentation
        load_samplers unless self['disable_samplers']
        local_env.gather_environment_info
        append_environment_info
      elsif !agent_enabled?
        install_shim
      end
    end
    
    # Install the real agent into the Agent module, and issue the start command.
    def start_agent
      NewRelic::Agent.agent.start
    end
    
    def [](key)
      fetch(key)
    end
    
    def settings
      unless @settings
        @settings = (@yaml && merge_defaults(@yaml[env])) || {}
        # At the time we bind the settings, we also need to run this little piece
        # of magic which allows someone to augment the id with the app name, necessary
        if self['multi_homed'] && app_names.size > 0
          if @local_env.dispatcher_instance_id
            @local_env.dispatcher_instance_id << ":#{app_names.first}"
          else
            @local_env.dispatcher_instance_id = app_names.first
          end
        end
        
      end
      @settings
    end
    
    def []=(key, value)
      settings[key] = value
    end
    
    def fetch(key, default=nil)
      settings.fetch(key, default)
    end
    # Add your own environment value to track for change detection.
    # The name and value should be stable and not vary across app processes on 
    # the same host.
    def append_environment_info(name, value)
      local_env.record_environment_info(name,value)
    end
    
    ###################################
    # Agent config conveniences
    
    def apdex_t
      # Always initialized with a default
      fetch('apdex_t').to_f
    end
    def license_key
      fetch('license_key')
    end
    def capture_params
      fetch('capture_params')
    end
    # True if we are sending data to the server, monitoring production
    def monitor_mode?
      fetch('monitor_mode', fetch('enabled'))
    end
    # True if we are capturing data and displaying in /newrelic
    def developer_mode?
      fetch('developer_mode', fetch('developer'))
    end
    # True if the app runs in multi-threaded mode
    def multi_threaded?
      fetch('multi_threaded')
    end
    # True if we should view files in textmate
    def use_textmate?
      fetch('textmate')
    end
    def post_size_limit
      fetch('post_size_limit', 2 * 1024 * 1024)
    end
    
    def sync_startup
      fetch('sync_startup', false)
    end
    # True if dev mode or monitor mode are enabled, and we are running
    # inside a valid dispatcher like mongrel or passenger.  Can be overridden
    # by NEWRELIC_ENABLE env variable, monitor_daemons config option when true, or
    # agent_enabled config option when true or false.
    def agent_enabled?
      return false if !developer_mode? && !monitor_mode?
      return self['agent_enabled'].to_s =~ /true|on|yes/i if !self['agent_enabled'].nil? && self['agent_enabled'] != 'auto'
      return false if ENV['NEWRELIC_ENABLE'].to_s =~ /false|off|no/i 
      return true if self['monitor_daemons'].to_s =~ /true|on|yes/i
      return true if ENV['NEWRELIC_ENABLE'].to_s =~ /true|on|yes/i
      # When in 'auto' mode the agent is enabled if there is a known
      # dispatcher running
      return true if @local_env.dispatcher != nil
    end
    
    def app
      @local_env.framework
    end
    alias framework app
    
    def dispatcher_instance_id
      self['dispatcher_instance_id'] || @local_env.dispatcher_instance_id
    end
    def dispatcher
      (self['dispatcher'] && self['dispatcher'].to_sym) || @local_env.dispatcher
    end
    def app_names
      self['app_name'] ? self['app_name'].split(';') : []
    end
    def validate_seed
      self['validate_seed'] || ENV['NR_VALIDATE_SEED']   
    end
    def validate_token
      self['validate_token'] || ENV['NR_VALIDATE_TOKEN']
    end
    
    def use_ssl?
      @use_ssl ||= fetch('ssl', false)
    end
    
    def verify_certificate?
      #this can only be on when SSL is enabled
      @verify_certificate ||= ( use_ssl? ? fetch('verify_certificate', false) : false)
    end

    def server
      @remote_server ||= server_from_host(nil)  
    end
    
    def api_server
      api_host = self['api_host'] || 'rpm.newrelic.com' 
      @api_server ||= 
      NewRelic::Control::Server.new \
      api_host, 
       (self['api_port'] || self['port'] || (use_ssl? ? 443 : 80)).to_i, 
      nil
    end
    
    def proxy_server
      @proxy_server ||=
      NewRelic::Control::ProxyServer.new self['proxy_host'], self['proxy_port'], self['proxy_user'], self['proxy_pass'] 
    end
    
    def server_from_host(hostname=nil)
      host = hostname || self['host'] || 'collector.newrelic.com'
      
      # if the host is not an IP address, turn it into one
      NewRelic::Control::Server.new host, (self['port'] || (use_ssl? ? 443 : 80)).to_i, convert_to_ip_address(host) 
    end
    
    # Return the Net::HTTP with proxy configuration given the NewRelic::Control::Server object.
    # Default is the collector but for api calls you need to pass api_server
    #
    # Experimental support for SSL verification:
    # swap 'VERIFY_NONE' for 'VERIFY_PEER' line to try it out
    # If verification fails, uncomment the 'http.ca_file' line
    # and it will use the included certificate.
    def http_connection(host = nil)
      host ||= server
      # Proxy returns regular HTTP if @proxy_host is nil (the default)
      http_class = Net::HTTP::Proxy(proxy_server.name, proxy_server.port, 
                                    proxy_server.user, proxy_server.password)
      http = http_class.new(host.ip || host.name, host.port)
      log.debug("Http Connection opened to #{host.ip||host.name}:#{host.port}")
      if use_ssl?
        http.use_ssl = true
        if verify_certificate?
          http.verify_mode = OpenSSL::SSL::VERIFY_PEER
          http.ca_file = File.join(File.dirname(__FILE__), '..', '..', 'cert', 'cacert.pem')
        else
          http.verify_mode = OpenSSL::SSL::VERIFY_NONE
        end
      end
      http
    end
    def to_s
      "Control[#{self.app}]"
    end
    
    def log
      # If we try to get a log before one has been set up, return a stdout log
      unless @log
        l = Logger.new(STDOUT)
        l.level = Logger::INFO
        return l
      end
      @log
    end
    
    # send the given message to STDOUT so that it shows
    # up in the console.  This should be used for important informational messages at boot.
    # The to_stdout may be implemented differently by different config subclasses.
    # This will NOT print anything if tracers are not enabled
    def log!(msg, level=:info)
      return if @settings && !agent_enabled?
      to_stdout msg
      log.send level, msg if @log
    end
    
    # Install stubs to the proper location so the app code will not fail
    # if the agent is not running.
    def install_shim
      # Once we install instrumentation, you can't undo that by installing the shim.
      raise "Cannot install the Agent shim after instrumentation has already been installed!" if @instrumented
      NewRelic::Agent.agent = NewRelic::Agent::ShimAgent.instance
    end
    
    # Add instrumentation.  Don't call this directly.  Use NewRelic::Agent#add_instrumentation.
    # This will load the file synchronously if we've already loaded the default
    # instrumentation.
    #
    def add_instrumentation pattern
      if @instrumented
        load_instrumentation_files pattern
      else
        @instrumentation_files << pattern
      end
    end
    def install_instrumentation
      return if @instrumented
      
      @instrumented = true
      
      # Instrumentation for the key code points inside rails for monitoring by NewRelic.
      # note this file is loaded only if the newrelic agent is enabled (through config/newrelic.yml)
      instrumentation_path = File.join(File.dirname(__FILE__), 'agent','instrumentation')
      @instrumentation_files <<
      File.join(instrumentation_path, '*.rb') <<
      File.join(instrumentation_path, app.to_s, '*.rb')
      @instrumentation_files.each { | pattern |  load_instrumentation_files pattern }
      log.debug "Finished instrumentation"
    end
    
    def load_samplers
      agent = NewRelic::Agent.instance
      NewRelic::Agent::Sampler.sampler_classes.each do | subclass |
        begin
          log.debug "#{subclass.name} not supported on this platform." and next if not subclass.supported_on_this_platform?
          sampler = subclass.new
          if subclass.use_harvest_sampler?
            agent.stats_engine.add_harvest_sampler sampler
            log.debug "Registered #{subclass.name} for harvest time sampling"
          else            
            agent.stats_engine.add_sampler sampler
            log.debug "Registered #{subclass.name} for periodic sampling"
          end
        rescue NewRelic::Agent::Sampler::Unsupported => e
          log.info "#{subclass} sampler not available: #{e}"
        rescue => e
          log.error "Error registering sampler: #{e}, #{e.backtrace.join("\n")}"
        end
      end
    end
    
    protected
    
    # Append framework specific environment information for uploading to
    # the server for change detection.  Override in subclasses
    def append_environment_info; end
    
    # Look up the ip address of the host using the pure ruby lookup 
    # to prevent blocking.  If that fails, fall back to the regular
    # IPSocket library.  Return nil if we can't find the host ip
    # address and don't have a good default.
    def convert_to_ip_address(host)
      # here we leave it as a host name since the cert verification
      # needs it in host form
      return host if verify_certificate?
      return nil if host.nil? || host.downcase == "localhost"
      # Fall back to known ip address in the common case
      ip_address = '65.74.177.195' if host.downcase == 'collector.newrelic.com'
      begin
        ip_address = Resolv.getaddress(host)
        log.info "Resolved #{host} to #{ip_address}"
      rescue => e
        log.warn "DNS Error caching IP address: #{e}"
        log.debug e.backtrace.join("\n   ")
        ip_address = IPSocket::getaddress host rescue ip_address
      end
      ip_address
    end
    
    def merge_defaults(settings_hash)
      s = {
        'host' => 'collector.newrelic.com',
        'ssl' => false,
        'log_level' => 'info',
        'apdex_t' => 1.0
      }
      s.merge! settings_hash if settings_hash
      # monitor_daemons replaced with agent_enabled
      s['agent_enabled'] = s.delete('monitor_daemons') if s['agent_enabled'].nil? && s.include?('monitor_daemons')
      s
    end
    
    # Control subclasses may override this, but it can be called multiple times.
    def setup_log
      @log_file = "#{log_path}/#{log_file_name}"
      @log = Logger.new @log_file
      
      # change the format just for our logger
      
      def @log.format_message(severity, timestamp, progname, msg)
        "[#{timestamp.strftime("%m/%d/%y %H:%M:%S %z")} #{Socket.gethostname} (#{$$})] #{severity} : #{msg}\n" 
      end
      
      # set the log level as specified in the config file
      case fetch("log_level","info").downcase
      when "debug" then @log.level = Logger::DEBUG
      when "info" then @log.level = Logger::INFO
      when "warn" then @log.level = Logger::WARN
      when "error" then @log.level = Logger::ERROR
      when "fatal" then @log.level = Logger::FATAL
      else @log.level = Logger::INFO
      end
      @log
    end
    
    def to_stdout(msg)
      STDOUT.puts "** [NewRelic] " + msg 
    end
    
    def config_file
      File.expand_path(File.join(root,"config","newrelic.yml"))
    end
    
    def log_path
      @log_path ||= begin
                      path = self['log_file_path'] || File.join(root,'log')
                      unless File.directory? path
                        path = '.'
                      end
                      File.expand_path(path)
                    end
    end

    def log_file_name
      @log_file_name ||= fetch('log_file_name', 'newrelic_agent.log')
    end
    
    # Create the concrete class for environment specific behavior:
    def self.new_instance
      @local_env = NewRelic::LocalEnvironment.new
      if @local_env.framework == :test
        require File.join(newrelic_root, "test", "config", "test_control.rb")
        NewRelic::Control::Test.new @local_env
      else
        begin
          require "new_relic/control/#{@local_env.framework}.rb"
        rescue LoadError
        end
        NewRelic::Control.const_get(@local_env.framework.to_s.capitalize).new @local_env
      end
    end
    
    def initialize local_env
      @local_env = local_env
      @instrumentation_files = []
      newrelic_file = config_file
      # Next two are for populating the newrelic.yml via erb binding, necessary
      # when using the default newrelic.yml file
      generated_for_user = ''
      license_key=''
      if !File.exists?(config_file)
        log! "Cannot find newrelic.yml file at #{config_file}."
        @yaml = {}
      else
        @yaml = YAML.load(ERB.new(File.read(config_file)).result(binding))
      end
    rescue ScriptError, StandardError => e
      puts e
      puts e.backtrace.join("\n")
      raise "Error reading newrelic.yml file: #{e}"
    end
    
    # The root directory for the plugin or gem
    def self.newrelic_root
      File.expand_path(File.join(File.dirname(__FILE__),"..",".."))
    end
    def newrelic_root
      self.class.newrelic_root
    end
    
    # Merge the given options into the config options.
    # They might be a nested hash
    def merge_options(options, hash=self)
      options.each do |key, val |
        case
        when key == :config then next 
        when val.is_a?(Hash)
          merge_options(val, hash[key.to_s] ||= {})
        when val.nil?
          hash.delete(key.to_s)
        else 
          hash[key.to_s] = val
        end
      end
    end
    
    def load_instrumentation_files pattern
      Dir.glob(pattern) do |file|
        begin
          log.debug "Processing instrumentation file '#{file}'"
          require file
        rescue => e
          log.error "Error loading instrumentation file '#{file}': #{e}"
          log.debug e.backtrace.join("\n")
        end
      end
    end
    
  end
end
