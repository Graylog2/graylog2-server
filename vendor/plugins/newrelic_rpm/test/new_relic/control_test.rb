require File.expand_path(File.join(File.dirname(__FILE__),'/../test_helper'))

class NewRelic::ControlTest < Test::Unit::TestCase
  
  attr_reader :c
  
  def setup
    NewRelic::Agent.manual_start
    @c =  NewRelic::Control.instance
  end
  def shutdown
    NewRelic::Agent.shutdown
  end

  def test_monitor_mode
    assert ! @c.monitor_mode?
    @c.settings.delete 'enabled'
    @c.settings.delete 'monitor_mode'
    assert !@c.monitor_mode?
    @c['enabled'] = false
    assert ! @c.monitor_mode?
    @c['enabled'] = true
    assert @c.monitor_mode?
    @c['monitor_mode'] = nil
    assert !@c.monitor_mode?
    @c['monitor_mode'] = false
    assert !@c.monitor_mode?
    @c['monitor_mode'] = true
    assert @c.monitor_mode?
  ensure
    @c['enabled'] = false
    @c['monitor_mode'] = false
  end
  
  def test_test_config
    assert_equal :rails, c.app
    assert_equal :test, c.framework
    assert_match /test/i, c.dispatcher_instance_id
    assert_equal nil, c.dispatcher
    assert !c['enabled']
    assert_equal false, c['monitor_mode']
    c.local_env
  end
  
  def test_root
    assert File.directory?(NewRelic::Control.newrelic_root), NewRelic::Control.newrelic_root
    assert File.directory?(File.join(NewRelic::Control.newrelic_root, "lib")), NewRelic::Control.newrelic_root +  "/lib"
  end
  
  def test_info
    props = NewRelic::Control.instance.local_env.snapshot
    assert_match /jdbc|postgres|mysql|sqlite/, props.assoc('Database adapter').last
  end
  
  def test_resolve_ip
    assert_equal nil, c.send(:convert_to_ip_address, 'localhost')
    assert_equal nil, c.send(:convert_to_ip_address, 'q1239988737.us')
    # This might fail if you don't have a valid, accessible, DNS server
    assert_equal '65.74.177.194', c.send(:convert_to_ip_address, 'rpm.newrelic.com')
  end
  def test_config_yaml_erb
    assert_equal 'heyheyhey', c['erb_value']
    assert_equal '', c['message']
    assert_equal '', c['license_key']
  end
  
  def test_config_booleans
    assert_equal c['tval'], true
    assert_equal c['fval'], false
    assert_nil c['not_in_yaml_val']
    assert_equal c['yval'], true 
    assert_equal c['sval'], 'sure'
  end
  def test_config_apdex
    assert_equal 1.1, c.apdex_t
  end
  def test_transaction_threshold
    assert_equal 'Apdex_f', c['transaction_tracer']['transaction_threshold']
    assert_equal 4.4, NewRelic::Agent::Agent.instance.instance_variable_get('@slowest_transaction_threshold')
  end
  def test_log_file_name
    assert_match /newrelic_agent.log$/, c.instance_variable_get('@log_file')
  end
   
  def test_transaction_threshold__apdex
    forced_start
    assert_equal 'Apdex_f', c['transaction_tracer']['transaction_threshold']
    assert_equal 4.4, NewRelic::Agent::Agent.instance.instance_variable_get('@slowest_transaction_threshold')
  end
  
  def test_transaction_threshold__default
    
    forced_start :transaction_tracer => { :transaction_threshold => nil}
    assert_nil c['transaction_tracer']['transaction_threshold']
    assert_equal 2.0, NewRelic::Agent::Agent.instance.instance_variable_get('@slowest_transaction_threshold')
  end
  
  def test_transaction_threshold__override
    forced_start :transaction_tracer => { :transaction_threshold => 1}
    assert_equal 1, c['transaction_tracer']['transaction_threshold']
    assert_equal 1, NewRelic::Agent::Agent.instance.instance_variable_get('@slowest_transaction_threshold')
  end
  def test_merging_options
    NewRelic::Control.send :public, :merge_options
    @c.merge_options :api_port => 66, :transaction_tracer => { :explain_threshold => 2.0 }
    assert_equal 66, NewRelic::Control.instance['api_port']
    assert_equal 2.0, NewRelic::Control.instance['transaction_tracer']['explain_threshold']
    assert_equal 'raw', NewRelic::Control.instance['transaction_tracer']['record_sql']
  end
  private
  def forced_start overrides = {}
    NewRelic::Agent.manual_start overrides
    # This is to force the agent to start again. 
    NewRelic::Agent.instance.stubs(:started?).returns(nil)
    NewRelic::Agent.instance.start 
  end
end
