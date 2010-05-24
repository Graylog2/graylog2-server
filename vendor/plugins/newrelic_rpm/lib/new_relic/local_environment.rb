require 'set'
require 'new_relic/version'

module NewRelic 
  # An instance of LocalEnvironment is responsible for determining
  # three things: 
  #
  # * Framework - :rails, :rails3, :merb, :ruby, :external, :test
  # * Dispatcher - A supported dispatcher, or nil (:mongrel, :thin, :passenger, :webrick, etc)
  # * Dispatcher Instance ID, which distinguishes agents on a single host from each other
  #
  # If the environment can't be determined, it will be set to
  # nil and dispatcher_instance_id will have nil.
  # 
  # NewRelic::LocalEnvironment should be accessed through NewRelic::Control#env (via the NewRelic::Control singleton).
  class LocalEnvironment

    attr_accessor :dispatcher # mongrel, thin, webrick, or possibly nil
    attr_accessor :dispatcher_instance_id # used to distinguish instances of a dispatcher from each other, may be nil
    attr_accessor :framework # rails, rails3, merb, external, ruby, test
    attr_reader :mongrel    # The mongrel instance, if there is one, captured as a convenience
    attr_reader :processors # The number of cpus, if detected, or nil
    alias environment dispatcher
    
    def initialize
      # Extend self with any any submodules of LocalEnvironment.  These can override
      # the discover methods to discover new framworks and dispatchers.  
      NewRelic::LocalEnvironment.constants.each do | const |
        mod = NewRelic::LocalEnvironment.const_get const
        self.extend mod if mod.instance_of? Module
      end
      
      discover_framework
      discover_dispatcher
      @dispatcher = nil if @dispatcher == :none
      @gems = Set.new
      @plugins = Set.new
      @config = Hash.new
    end

    # Add the given key/value pair to the app environment 
    # settings.  Must pass either a value or a block.  Block
    # is called to get the value and any raised errors are
    # silently ignored.
    def append_environment_value name, value = nil
      value = yield if block_given? 
      @config[name] = value if value
    rescue Exception
      # puts "#{e}\n  #{e.backtrace.join("\n  ")}" 
      raise if @framework == :test 
    end

    def append_gem_list
      @gems += yield
    rescue Exception => e
      # puts "#{e}\n  #{e.backtrace.join("\n  ")}"
      raise if @framework == :test 
    end
  
    def append_plugin_list
      @plugins += yield
    rescue Exception
      # puts "#{e}\n  #{e.backtrace.join("\n  ")}" 
      raise if @framework == :test 
    end
    
    def dispatcher_instance_id
      if @dispatcher_instance_id.nil?
        if @dispatcher.nil?
          @dispatcher_instance_id = File.basename($0).split(".").first
        end
      end
      @dispatcher_instance_id
    end
        
    # Collect base statistics about the environment and record them for
    # comparison and change detection.
    def gather_environment_info
      append_environment_value 'Framework', @framework.to_s
      append_environment_value 'Dispatcher', @dispatcher.to_s if @dispatcher
      append_environment_value 'Dispatcher instance id', @dispatcher_instance_id if @dispatcher_instance_id
      # This just creates a lot of keys
      # append_environment_value('Application root') { File.expand_path(NewRelic::Control.instance.root) }
      append_environment_value('Ruby version'){ RUBY_VERSION }
      append_environment_value('Ruby description'){ RUBY_DESCRIPTION } if defined? ::RUBY_DESCRIPTION
      append_environment_value('Ruby platform') { RUBY_PLATFORM }
      append_environment_value('Ruby patchlevel') { RUBY_PATCHLEVEL }
      if defined? ::JRUBY_VERSION
        append_environment_value('JRuby version') { JRUBY_VERSION }
        append_environment_value('Java VM version') { ENV_JAVA['java.vm.version']}
      end
      append_environment_value('OS version') { `uname -v` }
      append_environment_value('OS') { `uname -s` } ||
      append_environment_value('OS') { ENV['OS'] } 
      append_environment_value('Arch') { `uname -p` } ||
      append_environment_value('Arch') { ENV['PROCESSOR_ARCHITECTURE'] }
      # See what the number of cpus is, works only on linux.
      @processors = append_environment_value('Processors') do
        processors = File.readlines('/proc/cpuinfo').select { |line| line =~ /^processor\s*:/ }.size
        raise "Cannot determine the number of processors in /proc/cpuinfo" unless processors > 0
        processors
      end if File.readable? '/proc/cpuinfo'
      # The current Rails environment (development, test, or production).
      append_environment_value('Environment') { NewRelic::Control.instance.env }
      # Look for a capistrano file indicating the current revision:
      rev_file = File.join(NewRelic::Control.instance.root, "REVISION")
      if File.readable?(rev_file) && File.size(rev_file) < 64
        append_environment_value('Revision') do
          File.open(rev_file) { | file | file.readline.strip }
        end
      end
      # The name of the database adapter for the current environment.
      if defined? ::ActiveRecord
        append_environment_value 'Database adapter' do
          ActiveRecord::Base.configurations[RAILS_ENV]['adapter']
        end
        append_environment_value 'Database schema version' do
          ActiveRecord::Migrator.current_version
        end
      end
      if defined? ::DataMapper
        append_environment_value 'DataMapper version' do
          require 'dm-core/version'
          DataMapper::VERSION
        end
      end
    end
    # Take a snapshot of the environment information for this application
    # Returns an associative array
    def snapshot
      i = @config.to_a
      i << [ 'Plugin List', @plugins.to_a] if not @plugins.empty? 
      i << [ 'Gems', @gems.to_a] if not @gems.empty?
      i
    end
    
    def mongrel
      # Note that the odd defined? sequence is necessary to work around a bug in an older version
      # of JRuby.
      return @mongrel if @mongrel || ! (defined?(::Mongrel) && defined?(::Mongrel::HttpServer)) 
      ObjectSpace.each_object(Mongrel::HttpServer) do |mongrel|
        @mongrel = mongrel
      end unless defined?(::JRuby) && !JRuby.runtime.is_object_space_enabled
      @mongrel
    end
    
    def unicorn
      return @unicorn if @unicorn || ! (defined?(::Unicorn) && defined?(::Unicorn::HttpServer)) 
      ObjectSpace.each_object(Unicorn::HttpServer) do |unicorn|
        @unicorn = unicorn
      end unless defined?(::JRuby) && !JRuby.runtime.is_object_space_enabled
      @unicorn
    end

    # Obsolete method for DelayedJob instrumentation support, which is
    # now in the rpm_contrib gem.
    def delayed_worker=(worker)
      $stderr.puts "WARNING: DelayedJob instrumentation has been moved to the rpm_contrib gem."
    end

    private
    
    # Although you can override the framework with NEWRELIC_DISPATCHER this
    # is not advisable since it implies certain api's being available.
    def discover_dispatcher
      @dispatcher ||= ENV['NEWRELIC_DISPATCHER'] && ENV['NEWRELIC_DISPATCHER'].to_sym
      dispatchers = %w[passenger torquebox glassfish thin mongrel litespeed webrick fastcgi unicorn sinatra]
      while dispatchers.any? && @dispatcher.nil?
        send 'check_for_'+(dispatchers.shift)
      end
    end
    
    def discover_framework
      # Although you can override the framework with NEWRELIC_FRAMEWORK this
      # is not advisable since it implies certain api's being available.
      #
      # Note that the odd defined? sequence is necessary to work around a bug in an older version
      # of JRuby.
      @framework ||= case
        when ENV['NEWRELIC_FRAMEWORK'] then ENV['NEWRELIC_FRAMEWORK'].to_sym
        when defined?(::NewRelic::TEST) then :test
        when defined?(::Merb) && defined?(::Merb::Plugins) then :merb
        when defined?(::Rails) then check_rails_version
        when defined?(::Sinatra) && defined?(::Sinatra::Base) then :sinatra      
        when defined?(::NewRelic::IA) then :external
      else :ruby
      end      
    end

    def check_rails_version
      if Rails::VERSION::MAJOR < 3     
        :rails
      else
        :rails3
      end
    end

    def check_for_torquebox
      return unless defined?(::JRuby) &&
         ( Java::OrgTorqueboxRailsWebDeployers::RailsRackDeployer rescue nil) 
      @dispatcher = :torquebox
    end

    def check_for_glassfish
      return unless defined?(::JRuby) &&
        (((com.sun.grizzly.jruby.rack.DefaultRackApplicationFactory rescue nil) &&
          defined?(com::sun::grizzly::jruby::rack::DefaultRackApplicationFactory)) ||
         ((org.jruby.rack.DefaultRackApplicationFactory rescue nil) &&
          defined?(org::jruby::rack::DefaultRackApplicationFactory)) ||
         defined?(::GlassFish::Server))
      @dispatcher = :glassfish
    end

    def check_for_webrick
      return unless defined?(::WEBrick) && defined?(::WEBrick::VERSION)
      @dispatcher = :webrick
      if defined?(::OPTIONS) && OPTIONS.respond_to?(:fetch) 
        # OPTIONS is set by script/server
        @dispatcher_instance_id = OPTIONS.fetch(:port)
      end
      @dispatcher_instance_id = default_port unless @dispatcher_instance_id
    end
    
    def check_for_fastcgi
      return unless defined?(::FCGI)
      @dispatcher = :fastcgi
    end

    # this case covers starting by mongrel_rails
    def check_for_mongrel
      return unless defined?(::Mongrel) && defined?(::Mongrel::HttpServer)
      @dispatcher = :mongrel
      
      # Get the port from the server if it's started

      if mongrel && mongrel.respond_to?(:port)
        @dispatcher_instance_id = mongrel.port.to_s
      end
      
      # Get the port from the configurator if one was created
      if @dispatcher_instance_id.nil? && defined?(::Mongrel::Configurator)
        ObjectSpace.each_object(Mongrel::Configurator) do |mongrel|
          @dispatcher_instance_id = mongrel.defaults[:port] && mongrel.defaults[:port].to_s
        end unless defined?(::JRuby) && !JRuby.runtime.is_object_space_enabled
      end
      
      # Still can't find the port.  Let's look at ARGV to fall back
      @dispatcher_instance_id = default_port if @dispatcher_instance_id.nil?
    end
    
    def check_for_unicorn
      return unless defined?(::Unicorn) && defined?(::Unicorn::HttpServer)
      
      # unlike mongrel, unicorn manages muliple threads and ports, so we 
      # have to map multiple processes into one instance, as we do with passenger
      @dispatcher = :unicorn
    end
      
    def check_for_sinatra
      return unless defined?(::Sinatra) && defined?(::Sinatra::Base)
      
      begin
        version = ::Sinatra::VERSION
      rescue
        $stderr.puts("Error determining Sinatra version")
      end

      if ::NewRelic::VersionNumber.new('0.9.2') > version
        $stderr.puts("Your Sinatra version is #{version}, we highly recommend upgrading to >=0.9.2")
      end

      @dispatcher = :sinatra
    end
    
    def check_for_thin
      if defined?(::Thin) && defined?(::Thin::Server)
        # This case covers the thin web dispatcher
        # Same issue as above- we assume only one instance per process
        ObjectSpace.each_object(Thin::Server) do |thin_dispatcher|
          @dispatcher = :thin
          backend = thin_dispatcher.backend
          # We need a way to uniquely identify and distinguish agents.  The port
          # works for this.  When using sockets, use the socket file name.
          if backend.respond_to? :port
            @dispatcher_instance_id = backend.port
          elsif backend.respond_to? :socket
            @dispatcher_instance_id = backend.socket
          else
            raise "Unknown thin backend: #{backend}"
          end
        end # each thin instance
      end
      if defined?(::Thin) && defined?(::Thin::VERSION) && !@dispatcher
        @dispatcher = :thin
        @dispatcher_instance_id = default_port
      end
    end
    
    def check_for_litespeed
      if caller.pop =~ /fcgi-bin\/RailsRunner\.rb/
        @dispatcher = :litespeed
      end
    end
    
    def check_for_passenger
      if (defined?(::Passenger) && defined?(::Passenger::AbstractServer)) || defined?(::IN_PHUSION_PASSENGER) 
        @dispatcher = :passenger
      end
    end
    

    def default_port
      require 'optparse'
      # If nothing else is found, use the 3000 default
      default_port = 3000
      OptionParser.new do |opts|
        opts.on("-p", "--port=port", String) { | p | default_port = p }
        opts.parse(ARGV.clone) rescue nil
      end
      default_port
    end
    
    public 
    def to_s
      s = "LocalEnvironment["
      s << @framework.to_s
      s << ";dispatcher=#{@dispatcher}" if @dispatcher
      s << ";instance=#{@dispatcher_instance_id}" if @dispatcher_instance_id
      s << "]"
    end

  end
end
