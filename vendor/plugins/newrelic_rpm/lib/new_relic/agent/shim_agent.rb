# This agent is loaded by the plug when the plug-in is disabled
# It recreates just enough of the API to not break any clients that
# invoke the Agent.
class NewRelic::Agent::ShimAgent < NewRelic::Agent::Agent
  def self.instance
    @instance ||= self.new
  end  
  def initialize
    super
    @histogram.extend NewRelic::Histogram::Shim
    @stats_engine.extend NewRelic::Agent::StatsEngine::Shim
    @stats_engine.extend NewRelic::Agent::StatsEngine::Transactions::Shim
    @transaction_sampler.extend NewRelic::Agent::TransactionSampler::Shim
    @error_collector.extend NewRelic::Agent::ErrorCollector::Shim
  end
  def after_fork *args; end
  def start *args; end
  def shutdown; end
  def push_trace_execution_flag(*args); end
  def pop_trace_execution_flag(*args); end
end
