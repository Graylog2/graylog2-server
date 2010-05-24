require File.expand_path(File.join(File.dirname(__FILE__),'..','..','test_helper')) 
require 'active_record_fixtures'
class ActiveRecordInstrumentationTest < Test::Unit::TestCase
  include NewRelic::Agent::Instrumentation::ControllerInstrumentation
  def setup
    super
    NewRelic::Agent.manual_start
    ActiveRecordFixtures.setup
    NewRelic::Agent.instance.transaction_sampler.reset!
    NewRelic::Agent.instance.stats_engine.clear_stats
  rescue
    puts e
    puts e.backtrace.join("\n")
  end
  
  def teardown
    super
    ActiveRecordFixtures.teardown
    NewRelic::Agent.shutdown
  end
  
  def test_agent_setup
    assert NewRelic::Agent.instance.class == NewRelic::Agent::Agent
  end
  
  def test_finder
    ActiveRecordFixtures::Order.create :id => 0, :name => 'jeff'
    ActiveRecordFixtures::Order.find(:all)
    s = NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find")
    assert_equal 1, s.call_count
    ActiveRecordFixtures::Order.find_all_by_name "jeff"
    s = NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find")
    assert_equal 2, s.call_count
    ActiveRecordFixtures::Order.exists?(["name=?", 'jeff'])
    s = NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find")
    assert_equal 3, s.call_count if NewRelic::Control.instance.rails_version > '2.3.4'
  end
  
  # multiple duplicate find calls should only cause metric trigger on the first
  # call.  the others are ignored.
  def test_query_cache
    # Not sure why we get a transaction error with sqlite
    return if ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /sqlite/  
    ActiveRecordFixtures::Order.cache do
      m = ActiveRecordFixtures::Order.create :id => 0, :name => 'jeff'
      ActiveRecordFixtures::Order.find(:all)
      s = NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find")
      assert_equal 1, s.call_count
      
      10.times { ActiveRecordFixtures::Order.find m.id }
    end
    s = NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find")
    assert_equal 2, s.call_count    
  end
  
  def test_metric_names
    m = ActiveRecordFixtures::Order.create :id => 0, :name => 'jeff'
    m = ActiveRecordFixtures::Order.find(m.id)
    m.id = 999
    m.save!
    
    metrics = NewRelic::Agent.instance.stats_engine.metrics
    #   This doesn't work on hudson because the sampler metrics creep in.    
    #   metrics = NewRelic::Agent.instance.stats_engine.metrics.select { |mname| mname =~ /ActiveRecord\/ActiveRecordFixtures::Order\// }.sort
    expected = %W[
      ActiveRecord/all
      ActiveRecord/find
      ActiveRecord/ActiveRecordFixtures::Order/find
      ]
    expected += %W[Database/SQL/insert] if ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /jdbc/  
    expected += %W[ActiveRecord/create] unless  ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /jdbc/  
    expected += %W[Database/SQL/other] unless  ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /jdbc|sqlite/  
    expected += %W[ActiveRecord/ActiveRecordFixtures::Order/create] unless ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /jdbc/  
    expected += %W[ActiveRecord/save ActiveRecord/ActiveRecordFixtures::Order/save] if NewRelic::Control.instance.rails_version < '2.1.0'   
    compare_metrics expected, metrics
    assert_equal 1, NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find").call_count
    assert_equal (defined?(JRuby) ? 0 : 1), NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/create").call_count
  end
  def test_join_metrics
    m = ActiveRecordFixtures::Order.create :name => 'jeff'
    m = ActiveRecordFixtures::Order.find(m.id)
    s = m.shipments.create
    m.shipments.to_a
    m.destroy
    
    metrics = NewRelic::Agent.instance.stats_engine.metrics
    #   This doesn't work on hudson because the sampler metrics creep in.    
    #   metrics = NewRelic::Agent.instance.stats_engine.metrics.select { |mname| mname =~ /ActiveRecord\/ActiveRecordFixtures::Order\// }.sort
    expected_metrics = %W[
    ActiveRecord/all
    ActiveRecord/destroy
    ActiveRecord/ActiveRecordFixtures::Order/destroy
    Database/SQL/insert
    Database/SQL/delete
    ActiveRecord/find
    ActiveRecord/ActiveRecordFixtures::Order/find
    ActiveRecord/ActiveRecordFixtures::Shipment/find
    ]
    
    expected_metrics += %W[
    Database/SQL/other 
    Database/SQL/show
    ] unless ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /jdbc|sqlite/  
    expected_metrics += %W[
    ActiveRecord/create
    ActiveRecord/ActiveRecordFixtures::Shipment/create
    ActiveRecord/ActiveRecordFixtures::Order/create
    ] unless ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /jdbc/  
    
    compare_metrics expected_metrics, metrics
    # This number may be different with different db adapters, not sure
    # assert_equal 17, NewRelic::Agent.get_stats("ActiveRecord/all").call_count
    assert_equal NewRelic::Agent.get_stats("ActiveRecord/all").total_exclusive_time, NewRelic::Agent.get_stats("ActiveRecord/all").total_call_time unless defined?(RUBY_DESCRIPTION) && RUBY_DESCRIPTION =~ /Enterprise Edition/
    assert_equal 1, NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find").call_count
    assert_equal 1, NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Shipment/find").call_count
    assert_equal 1, NewRelic::Agent.get_stats("Database/SQL/insert").call_count unless defined? JRuby
    assert_equal 3, NewRelic::Agent.get_stats("Database/SQL/insert").call_count if defined? JRuby
    assert_equal 1, NewRelic::Agent.get_stats("Database/SQL/delete").call_count
  end
  def test_direct_sql
    assert_nil NewRelic::Agent::Instrumentation::MetricFrame.current
    assert_equal nil, NewRelic::Agent.instance.stats_engine.scope_name 
    assert_equal 0, NewRelic::Agent.instance.stats_engine.metrics.size, NewRelic::Agent.instance.stats_engine.metrics.inspect
    ActiveRecordFixtures::Order.connection.select_rows "select * from #{ActiveRecordFixtures::Order.table_name}"
    metrics = NewRelic::Agent.instance.stats_engine.metrics
    compare_metrics %W[
    ActiveRecord/all
    Database/SQL/select
    ], metrics
    assert_equal 1, NewRelic::Agent.instance.stats_engine.get_stats_no_scope("Database/SQL/select").call_count, NewRelic::Agent.instance.stats_engine.get_stats_no_scope("Database/SQL/select")
  end
  
  def test_other_sql
    list = ActiveRecordFixtures::Order.connection.execute "begin"
    metrics = NewRelic::Agent.instance.stats_engine.metrics
    compare_metrics %W[
    ActiveRecord/all
    Database/SQL/other
    ], metrics
    assert_equal 1, NewRelic::Agent.get_stats_no_scope("Database/SQL/other").call_count
  end
  
  def test_show_sql
    return if ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /sqlite/  
    list = ActiveRecordFixtures::Order.connection.execute "show tables"
    metrics = NewRelic::Agent.instance.stats_engine.metrics
    compare_metrics %W[
    ActiveRecord/all
    Database/SQL/show
    ], metrics
    assert_equal 1, NewRelic::Agent.get_stats_no_scope("Database/SQL/show").call_count 
  end

  def test_blocked_instrumentation
    ActiveRecordFixtures::Order.add_delay
    NewRelic::Agent.disable_all_tracing do
      perform_action_with_newrelic_trace :name => 'bogosity' do
        ActiveRecordFixtures::Order.find(:all)
      end
    end
    assert_nil NewRelic::Agent.instance.transaction_sampler.last_sample
    metrics = NewRelic::Agent.instance.stats_engine.metrics
    compare_metrics [], metrics
  end
  def test_run_explains
    perform_action_with_newrelic_trace :name => 'bogosity' do
      ActiveRecordFixtures::Order.add_delay
      ActiveRecordFixtures::Order.find(:all)
    end    
    sample = NewRelic::Agent.instance.transaction_sampler.last_sample
    
    segment = sample.root_segment.called_segments.first.called_segments.first.called_segments.first
    assert_match /^SELECT \* FROM ["`]?#{ActiveRecordFixtures::Order.table_name}["`]?$/i, segment.params[:sql].strip
    NewRelic::TransactionSample::Segment.any_instance.expects(:explain_sql).returns([])
    sample = sample.prepare_to_send(:obfuscate_sql => true, :explain_enabled => true, :explain_sql => 0.0)
    segment = sample.root_segment.called_segments.first.called_segments.first
  end
  def test_prepare_to_send
    perform_action_with_newrelic_trace :name => 'bogosity' do
      ActiveRecordFixtures::Order.add_delay
      ActiveRecordFixtures::Order.find(:all)
    end
    sample = NewRelic::Agent.instance.transaction_sampler.last_sample
    assert_not_nil sample
    assert_equal 3, sample.count_segments, sample.to_s
    # 
    sql_segment = sample.root_segment.called_segments.first.called_segments.first.called_segments.first rescue nil
    assert_not_nil sql_segment, sample.to_s
    assert_match /^SELECT /, sql_segment.params[:sql]
    assert sql_segment.duration > 0.0, "Segment duration must be greater than zero."
    sample = sample.prepare_to_send(:record_sql => :raw, :explain_enabled => true, :explain_sql => 0.0)
    sql_segment = sample.root_segment.called_segments.first.called_segments.first.called_segments.first
    assert_match /^SELECT /, sql_segment.params[:sql]
    explanations = sql_segment.params[:explanation]
    if isMysql? || isPostgres?
      assert_not_nil explanations, "No explains in segment: #{sql_segment}"
      assert_equal 1, explanations.size,"No explains in segment: #{sql_segment}" 
      assert_equal 1, explanations.first.size
    end
  end
  def test_transaction
    sample = NewRelic::Agent.instance.transaction_sampler.reset!
    perform_action_with_newrelic_trace :name => 'bogosity' do
      ActiveRecordFixtures::Order.add_delay
      ActiveRecordFixtures::Order.find(:all)
    end
    
    sample = NewRelic::Agent.instance.transaction_sampler.last_sample
    
    sample = sample.prepare_to_send(:obfuscate_sql => true, :explain_enabled => true, :explain_sql => 0.0)
    segment = sample.root_segment.called_segments.first.called_segments.first.called_segments.first
    assert_nil segment.params[:sql], "SQL should have been removed."
    explanations = segment.params[:explanation]
    if isMysql? || isPostgres?
      assert_not_nil explanations, "No explains in segment: #{segment}"
      assert_equal 1, explanations.size,"No explains in segment: #{segment}" 
      assert_equal 1, explanations.first.size
    end    
    if isPostgres?
      assert_equal Array, explanations.class
      assert_equal Array, explanations[0].class
      assert_equal Array, explanations[0][0].class
      assert_match /Seq Scan on test_data/, explanations[0][0].join(";") 
    elsif isMysql?
      assert_equal "1;SIMPLE;#{ActiveRecordFixtures::Order.table_name};ALL;;;;;1;", explanations.first.first.join(";")
    end
    
    s = NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find")
    assert_equal 1, s.call_count
  end
  # These are only valid for rails 2.1 and later
  if NewRelic::Control.instance.rails_version >= NewRelic::VersionNumber.new("2.1.0")
    ActiveRecordFixtures::Order.class_eval do
      named_scope :jeffs, :conditions => { :name => 'Jeff' }
    end
    def test_named_scope
      ActiveRecordFixtures::Order.create :name => 'Jeff'
      s = NewRelic::Agent.get_stats("ActiveRecord/ActiveRecordFixtures::Order/find")
      before_count = s.call_count
      x = ActiveRecordFixtures::Order.jeffs.find(:all)
      assert_equal 1, x.size
      se = NewRelic::Agent.instance.stats_engine
      assert_equal before_count+1, s.call_count
    end
  end
  
  # This is to make sure the all metric is recorded for exceptional cases
  def test_error_handling
    # have the AR select throw an error
    ActiveRecordFixtures::Order.connection.stubs(:log_info).with do | sql, x, y |
      raise "Error" if sql =~ /select/
      true
    end
    ActiveRecordFixtures::Order.connection.select_rows "select * from #{ActiveRecordFixtures::Order.table_name}" rescue nil
    metrics = NewRelic::Agent.instance.stats_engine.metrics
    compare_metrics %W[
    ActiveRecord/all
    Database/SQL/select
    ], metrics
    assert_equal 1, NewRelic::Agent.get_stats("Database/SQL/select").call_count
    assert_equal 1, NewRelic::Agent.get_stats("ActiveRecord/all").call_count
  end
  
  def test_rescue_handling
    # Not sure why we get a transaction error with sqlite
    return if ActiveRecord::Base.configurations[RAILS_ENV]['adapter'] =~ /sqlite/  
    begin
      ActiveRecordFixtures::Order.transaction do
        raise ActiveRecord::ActiveRecordError.new('preserve-me!') 
      end
    rescue ActiveRecord::ActiveRecordError => e
      assert_equal 'preserve-me!', e.message
    rescue
      fail "Rescue2: Got something COMPLETELY unexpected: $!:#{$!.inspect}"
    end
    
  end
  
  private
  
  def isPostgres?
    ActiveRecordFixtures::Order.configurations[RAILS_ENV]['adapter'] =~ /postgres/
  end
  def isMysql?
    ActiveRecordFixtures::Order.connection.class.name =~ /mysql/i 
  end
end
