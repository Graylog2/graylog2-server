# Control subclass instantiated when Rails is detected.  Contains
# Rails specific configuration, instrumentation, environment values, 
# etc.
class NewRelic::Control::Rails3 < NewRelic::Control
  
  def env
    @env ||= ::Rails.env.to_s
  end

  def root
    @root ||= Rails.root.to_s
  end

  def logger
    ::Rails.logger
  end
  
  def base_log_file
    logger.instance_eval {
      @log.path
    }
  rescue
    File.join(root, 'log')
  end
  private :base_log_file

  def log_path
    @log_path ||= File.expand_path(File.dirname(base_log_file))
  end

  def vendor_root
    @vendor_root ||= File.join(root,'vendor','rails')
  end
  
  def version
    @rails_version ||= NewRelic::VersionNumber.new(::Rails::VERSION::STRING)
  end

  def init_config(options={})
    rails_config=options[:config]
    if !agent_enabled?
      # Might not be running if it does not think mongrel, thin, passenger, etc
      # is running, if it things it's a rake task, or if the agent_enabled is false.
      logger.info "New Relic Agent not running."
    else
      logger.info "Starting the New Relic Agent."
    end
  end
  
  protected 
  
  # Collect the Rails::Info into an associative array as well as the list of plugins
  def append_environment_info
    local_env.append_environment_value('Rails version'){ version }
    local_env.append_environment_value('Rails threadsafe') do
      ::Rails.configuration.action_controller.allow_concurrency == true
    end
    local_env.append_environment_value('Rails Env') { env }
    local_env.append_gem_list do
      ::Rails.configuration.gems.map do | gem |
        version = (gem.respond_to?(:version) && gem.version) ||
          (gem.specification.respond_to?(:version) && gem.specification.version)
        gem.name + (version ? "(#{version})" : "")
      end
    end      
    local_env.append_plugin_list { ::Rails.configuration.plugins }
  end
  
  def install_shim
    super
    require 'new_relic/agent/instrumentation/controller_instrumentation'
    ActionController::Base.send :include, NewRelic::Agent::Instrumentation::ControllerInstrumentation::Shim
  end
  
end
