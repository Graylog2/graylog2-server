require 'new_relic/agent/instrumentation/controller_instrumentation'

if defined?(Delayed::Job) and not NewRelic::Control.instance['disable_dj']
  module NewRelic
    module Agent
      module Instrumentation
        module DelayedJobInstrumentation

          Delayed::Job.class_eval do
            include NewRelic::Agent::Instrumentation::ControllerInstrumentation
            if self.instance_methods.include?('name')
              add_transaction_tracer "invoke_job", :category => 'OtherTransaction/DelayedJob', :path => '#{self.name}'
            else
              add_transaction_tracer "invoke_job", :category => 'OtherTransaction/DelayedJob'
            end
          end
          
        end 
      end
    end
  end
end
