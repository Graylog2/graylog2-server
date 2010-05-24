module NewRelic
  module Agent
    module Instrumentation
      module Rails3
        module Errors
          def newrelic_notice_error(exception, custom_params = {})
            filtered_params = (respond_to? :filter_parameters) ? filter_parameters(params) : params
            filtered_params.merge!(custom_params)
            NewRelic::Agent.agent.error_collector.notice_error(exception, request, newrelic_metric_path, filtered_params)
          end
        end
      end
    end
  end
end

if defined?(ActionController) && defined?(ActionController::Base)
  class ActionController::Base
    include NewRelic::Agent::Instrumentation::Rails3::Errors
  end
end
