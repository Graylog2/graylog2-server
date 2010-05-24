require File.expand_path(File.join(File.dirname(__FILE__),'..','..','test_helper')) 
require 'action_controller/base'
require 'new_relic/agent/agent_test_controller'

class AgentControllerTest < ActionController::TestCase
  
  self.controller_class = NewRelic::Agent::AgentTestController
  
  attr_accessor :agent, :engine
  
  # Normally you can do this with #setup but for some reason in rails 2.0.2
  # setup is not called.
  def initialize name
    super name
    Thread.current[:newrelic_ignore_controller] = nil
    NewRelic::Agent.manual_start
    @agent = NewRelic::Agent.instance
    #    @agent.instrument_app
    agent.transaction_sampler.harvest
    NewRelic::Agent::AgentTestController.class_eval do
      newrelic_ignore :only => [:action_to_ignore, :entry_action, :base_action]
      newrelic_ignore_apdex :only => :action_to_ignore_apdex
    end
    @engine = @agent.stats_engine
  end
  
  def teardown
    Thread.current[:newrelic_ignore_controller] = nil
    super
    NewRelic::Agent.shutdown
    NewRelic::Agent::AgentTestController.clear_headers
  end
  
  def test_mongrel_queue
    engine.clear_stats
    NewRelic::Control.instance.local_env.stubs(:mongrel).returns( stub('mongrel', :workers => stub('workers', :list => stub('list', :length => '10'))))
    
    get :index
    assert_equal 1, stats('HttpDispatcher').call_count
    assert_equal 1, engine.get_stats_no_scope('Mongrel/Queue Length').call_count
    assert_equal 9, engine.get_stats_no_scope('Mongrel/Queue Length').total_call_time
    assert_equal 0, engine.get_stats_no_scope('WebFrontend/Mongrel/Average Queue Time').call_count
  end
  
  def test_heroku_queue
    engine.clear_stats
    NewRelic::Control.instance.local_env.stubs(:mongrel).returns( stub('mongrel', :workers => stub('workers', :list => stub('list', :length => '10'))))
    NewRelic::Agent::AgentTestController.set_some_headers 'HTTP_X_HEROKU_QUEUE_DEPTH'=>'15'
    get :index
    assert_equal 1, stats('HttpDispatcher').call_count
    assert_equal 1, engine.get_stats_no_scope('Mongrel/Queue Length').call_count
    assert_equal 15, engine.get_stats_no_scope('Mongrel/Queue Length').total_call_time
    assert_equal 0, engine.get_stats_no_scope('WebFrontend/Mongrel/Average Queue Time').call_count

  end
  def test_metric__ignore
    engine.clear_stats
    compare_metrics [], engine.metrics
    get :action_to_ignore
    compare_metrics [], engine.metrics
  end
  
  def test_controller_rescued_error
    engine.clear_stats
    assert_raise RuntimeError do
      get :action_with_error
    end
    metrics =  ['Apdex',
                'Apdex/new_relic/agent/agent_test/action_with_error',
                'HttpDispatcher',
                'Controller/new_relic/agent/agent_test/action_with_error',
                'Errors/all']

    compare_metrics metrics, engine.metrics.reject{|m| m.index('Response')==0 || m.index('CPU')==0}
    assert_equal 1, engine.get_stats_no_scope("Controller/new_relic/agent/agent_test/action_with_error").call_count
    assert_equal 1, engine.get_stats_no_scope("Errors/all").call_count
    apdex = engine.get_stats_no_scope("Apdex")
    score = apdex.get_apdex
    assert_equal 1, score[2], 'failing'
    assert_equal 0, score[1], 'tol'
    assert_equal 0, score[0], 'satisfied'
   
  end
  def test_controller_error
    engine.clear_stats
    assert_raise RuntimeError do
      get :action_with_error
    end
    metrics =  ['Apdex',
                'Apdex/new_relic/agent/agent_test/action_with_error',
                'HttpDispatcher',
                'Controller/new_relic/agent/agent_test/action_with_error',
                'Errors/all']

    compare_metrics metrics, engine.metrics.reject{|m| m.index('Response')==0 || m.index('CPU')==0}
    assert_equal 1, engine.get_stats_no_scope("Controller/new_relic/agent/agent_test/action_with_error").call_count
    assert_equal 1, engine.get_stats_no_scope("Errors/all").call_count
    apdex = engine.get_stats_no_scope("Apdex")
    score = apdex.get_apdex
    assert_equal 1, score[2], 'failing'
    assert_equal 0, score[1], 'tol'
    assert_equal 0, score[0], 'satisfied'

  end
  def test_filter_error
    engine.clear_stats
    assert_raise RuntimeError do
      get :action_with_before_filter_error
    end
    metrics =  ['Apdex',
                'Apdex/new_relic/agent/agent_test/action_with_before_filter_error',
                'HttpDispatcher',
                'Controller/new_relic/agent/agent_test/action_with_before_filter_error',
                'Errors/all']

    compare_metrics metrics, engine.metrics.reject{|m| m.index('Response')==0 || m.index('CPU')==0}
    assert_equal 1, engine.get_stats_no_scope("Controller/new_relic/agent/agent_test/action_with_before_filter_error").call_count
    assert_equal 1, engine.get_stats_no_scope("Errors/all").call_count
    apdex = engine.get_stats_no_scope("Apdex")
    score = apdex.get_apdex
    assert_equal 1, score[2], 'failing'
    assert_equal 0, score[1], 'tol'
    assert_equal 0, score[0], 'satisfied'
  end
  def test_metric__ignore_base
    engine.clear_stats
    get :base_action
    compare_metrics [], engine.metrics
  end
  def test_metric__no_ignore
    path = 'new_relic/agent/agent_test/index'
    index_stats = stats("Controller/#{path}")
    index_apdex_stats = engine.get_custom_stats("Apdex/#{path}", NewRelic::ApdexStats)
    assert_difference 'index_stats.call_count' do
      assert_difference 'index_apdex_stats.call_count' do
        get :index
      end
    end
    assert_nil Thread.current[:newrelic_ignore_controller]
  end
  def test_metric__ignore_apdex
    engine = @agent.stats_engine
    path = 'new_relic/agent/agent_test/action_to_ignore_apdex'
    cpu_stats = stats("ControllerCPU/#{path}")
    index_stats = stats("Controller/#{path}")
    index_apdex_stats = engine.get_custom_stats("Apdex/#{path}", NewRelic::ApdexStats)
    assert_difference 'index_stats.call_count' do
      assert_no_difference 'index_apdex_stats.call_count' do
        get :action_to_ignore_apdex
      end
    end
    assert_nil Thread.current[:newrelic_ignore_controller]
    
  end
  def test_metric__dispatched
    engine = @agent.stats_engine
    get :entry_action
    assert_nil Thread.current[:newrelic_ignore_controller]
    assert_nil engine.lookup_stat('Controller/agent_test/entry_action')
    assert_nil engine.lookup_stat('Controller/agent_test_controller/entry_action')
    assert_nil engine.lookup_stat('Controller/AgentTestController/entry_action')
    assert_nil engine.lookup_stat('Controller/NewRelic::Agent::AgentTestController/internal_action')
    assert_nil engine.lookup_stat('Controller/NewRelic::Agent::AgentTestController_controller/internal_action')
    assert_not_nil engine.lookup_stat('Controller/NewRelic::Agent::AgentTestController/internal_traced_action')
  end
  def test_action_instrumentation
    begin
      get :index, :foo => 'bar'
      assert_match /bar/, @response.body
      #rescue ActionController::RoutingError
      # you might get here if you don't have the default route installed.
    end
  end
  
  def test_controller_params
    assert agent.transaction_sampler
    num_samples = NewRelic::Agent.instance.transaction_sampler.samples.length
    assert_equal "[FILTERED]", @controller._filter_parameters({'social_security_number' => 'test'})['social_security_number']
    get :index, 'social_security_number' => "001-555-1212"
    samples = agent.transaction_sampler.samples
    assert_equal num_samples + 1, samples.length
    assert_equal "[FILTERED]", samples.last.params[:request_params]["social_security_number"]
  end
  
  def test_controller_params
    agent.transaction_sampler.reset!
    get :index, 'number' => "001-555-1212"
    s = agent.transaction_sampler.harvest(nil, 0.0)
    assert_equal 1, s.size
    assert_equal 5, s.first.params.size
  end

  
  def test_busycalculation
    engine.clear_stats
    
    assert_equal 0, NewRelic::Agent::BusyCalculator.busy_count
    get :index, 'social_security_number' => "001-555-1212", 'wait' => '1.0'
    NewRelic::Agent::BusyCalculator.harvest_busy

    assert_equal 1, stats('Instance/Busy').call_count
    assert_equal 1, stats('HttpDispatcher').call_count
    # We are probably busy about 99% of the time, but lets make sure it's at least 50
    assert stats('Instance/Busy').total_call_time > 0.5, stats('Instance/Busy')
    assert_equal 0, stats('WebFrontend/Mongrel/Average Queue Time').call_count
  end
  
  def test_histogram
    engine.clear_stats
    get :index, 'social_security_number' => "001-555-1212"
    bucket = NewRelic::Agent.instance.stats_engine.metrics.find { | m | m =~ /^Response Times/ }
    assert_not_nil bucket
    bucket_stats = stats(bucket)
    assert_equal 1, bucket_stats.call_count
  end

  def test_queue_headers
    engine.clear_stats
    queue_length_stat = stats('Mongrel/Queue Length')
    queue_time_stat = stats('WebFrontend/Mongrel/Average Queue Time')
    
    # no request start header
    get :index
    assert_equal 0, queue_length_stat.call_count

    # apache version of header
    request_start = ((Time.now.to_f - 0.2) * 1e6).to_i.to_s
    NewRelic::Agent::AgentTestController.set_some_headers({'HTTP_X_REQUEST_START' => "t=#{request_start}"})
    get :index
    assert_equal(0, queue_length_stat.call_count, 'We should not be seeing a queue length yet')
    assert_equal(1, queue_time_stat.call_count, 'We should have seen the queue header once')
    assert(queue_time_stat.total_call_time > 0.1, "Queue time should be longer than 100ms")
    assert(queue_time_stat.total_call_time < 10, "Queue time should be under 10 seconds (sanity check)")    
    
    engine.clear_stats
    NewRelic::Agent::AgentTestController.clear_headers    

    queue_length_stat = stats('Mongrel/Queue Length')
    queue_time_stat = stats('WebFrontend/Mongrel/Average Queue Time')
    
    # heroku version
    request_start = ((Time.now.to_f - 0.2) * 1e3).to_i.to_s
    NewRelic::Agent::AgentTestController.set_some_headers({'HTTP_X_REQUEST_START' => request_start, 'HTTP_X_HEROKU_QUEUE_DEPTH' => '0'})
    get :index
    assert_equal(0, queue_length_stat.total_call_time, 'queue should be empty')
    assert_equal(1, queue_time_stat.call_count, 'should have seen the queue header once')
    assert(queue_time_stat.total_call_time > 0.1, "Queue time should be longer than 100ms")
    assert(queue_time_stat.total_call_time < 10, "Queue time should be under 10 seconds (sanity check)")
    engine.clear_stats
    NewRelic::Agent::AgentTestController.clear_headers
    
    queue_length_stat = stats('Mongrel/Queue Length')
    queue_time_stat = stats('WebFrontend/Mongrel/Average Queue Time')    

    # heroku version with queue length > 0
    request_start = ((Time.now.to_f - 0.2) * 1e3).to_i.to_s
    NewRelic::Agent::AgentTestController.set_some_headers({'HTTP_X_REQUEST_START' => request_start, 'HTTP_X_HEROKU_QUEUE_DEPTH' => '3'})
    get :index
    
    assert_equal(1, queue_length_stat.call_count, 'queue should have been seen once')
    assert_equal(1, queue_time_stat.call_count, 'should have seen the queue header once')
    assert(queue_time_stat.total_call_time > 0.1, "Queue time should be longer than 100ms")
    assert(queue_time_stat.total_call_time < 10, "Queue time should be under 10 seconds (sanity check)")
    assert_equal(3, queue_length_stat.total_call_time, 'queue should be 3 long')

    NewRelic::Agent::AgentTestController.clear_headers
  end
  
  private
  def stats(name)
    engine.get_stats_no_scope(name)
  end
  
end
