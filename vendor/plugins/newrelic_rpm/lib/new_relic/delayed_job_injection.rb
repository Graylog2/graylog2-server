# This installs some code to manually start the agent when a delayed job worker starts.
# It's not really instrumentation.  It's more like a hook from DJ to RPM so it gets
# loaded at the time the RPM plugin initializes, which must be before the DJ worker
# initializes.  Loaded from control.rb
module NewRelic
  module DelayedJobInjection
    extend self
    attr_accessor :worker_name
  end
end

Delayed::Worker.class_eval do
  def initialize_with_new_relic(*args)
    initialize_without_new_relic(*args)
    worker_name = case
    when self.respond_to?(:name) then self.name
    when self.class.respond_to?(:default_name) then self.class.default_name
    end
    dispatcher_instance_id = worker_name || "host:#{Socket.gethostname} pid:#{Process.pid}" rescue "pid:#{Process.pid}"
    say "RPM Monitoring DJ worker #{dispatcher_instance_id}"
    NewRelic::DelayedJobInjection.worker_name = worker_name
    NewRelic::Agent.manual_start :dispatcher => :delayed_job, :dispatcher_instance_id => dispatcher_instance_id
  end
  
  alias initialize_without_new_relic initialize
  alias initialize initialize_with_new_relic
end if defined?(::Delayed::Worker) and not NewRelic::Control.instance['disable_dj']
