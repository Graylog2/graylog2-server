require File.expand_path(File.join(File.dirname(__FILE__),'..','..','test_helper')) 
require 'new_relic/agent/mock_ar_connection'
require 'test/unit'

::SQL_STATEMENT = "SELECT * from sandwiches"

NewRelic::TransactionSample::Segment.class_eval do
  def handle_exception_in_explain(e)
    fail "Got error in explain plan: #{e}"
  end
  
end
class NewRelic::TransationSampleTest < Test::Unit::TestCase
  include TransactionSampleTestHelper
  def setup
    NewRelic::Agent.manual_start
  end
  
  def test_sql
    assert ActiveRecord::Base.test_connection({}).disconnected == false
    
    t = make_sql_transaction(::SQL_STATEMENT, ::SQL_STATEMENT)
    
    s = t.prepare_to_send(:obfuscate_sql => true, :explain_enabled => true, :explain_sql => 0.00000001)
    
    explain_count = 0
    
    s.each_segment do |segment|
      if segment.params[:explanation]
        explanations = segment.params[:explanation]
        
        explanations.each do |explanation|
          assert_equal Array, explanation.class
          assert_equal "EXPLAIN #{::SQL_STATEMENT}", explanation[0][0]
          explain_count += 1
        end
      end
    end
    
    assert_equal 2, explain_count
    assert ActiveRecord::Base.test_connection({}).disconnected
  end
  
  
  def test_disable_sql
    t = nil
    NewRelic::Agent.disable_sql_recording do
      t = make_sql_transaction(::SQL_STATEMENT, ::SQL_STATEMENT)
    end
    
    s = t.prepare_to_send(:obfuscate_sql => true, :explain_sql => 0.00000001)
    
    s.each_segment do |segment|
      fail if segment.params[:explanation] || segment.params[:obfuscated_sql]
    end        
  end
  
  
  def test_disable_tt
    NewRelic::Agent.disable_transaction_tracing do
      t = make_sql_transaction(::SQL_STATEMENT, ::SQL_STATEMENT)
      assert t.nil?
    end
  end
  def test_enabled_tt
    t = make_sql_transaction(::SQL_STATEMENT, ::SQL_STATEMENT)
    assert t
  end
  
  def test_record_sql_off
    t = make_sql_transaction(::SQL_STATEMENT, ::SQL_STATEMENT)
    
    s = t.prepare_to_send(:obfuscate_sql => true, :explain_sql => 0.00000001, :record_sql => :off)
    
    s.each_segment do |segment|
      fail if segment.params[:explanation] || segment.params[:obfuscated_sql] || segment.params[:sql]
    end        
  end
  
  
  def test_record_sql_raw
    t = make_sql_transaction(::SQL_STATEMENT, ::SQL_STATEMENT)
    
    s = t.prepare_to_send(:obfuscate_sql => true, :explain_sql => 0.00000001, :record_sql => :raw)
    
    got_one = false
    s.each_segment do |segment|
      fail if segment.params[:obfuscated_sql]
      got_one = got_one || segment.params[:explanation] || segment.params[:sql]
    end
    
    assert got_one
  end
  
  
  def test_record_sql_obfuscated
    t = make_sql_transaction(::SQL_STATEMENT, ::SQL_STATEMENT)
    
    s = t.prepare_to_send(:obfuscate_sql => true, :explain_sql => 0.00000001, :record_sql => :obfuscated)
    
    got_one = false
    s.each_segment do |segment|
      fail if segment.params[:sql]
      got_one = got_one || segment.params[:explanation] || segment.params[:sql_obfuscated]
    end        
    
    assert got_one
  end
  
  
  def test_sql_throw
    ActiveRecord::Base.test_connection({}).throw = true
    
    t = make_sql_transaction(::SQL_STATEMENT, ::SQL_STATEMENT)
    
    # the sql connection will throw
    t.prepare_to_send(:obfuscate_sql => true, :explain_sql => 0.00000001)
  end
  

  def test_exclusive_duration
    t = nested_sample
    s1 = t.root_segment.called_segments.first
    assert_equal 3.0, s1.duration
    assert_equal 2.0, s1.exclusive_duration
  end
  
  def test_count
    assert_equal 4, nested_sample.count_segments
  end
  
  
  def test_truncate
    sample = nested_sample
    sample.truncate(2)
    assert_equal 2, sample.count_segments
    
    sample = large_sample
    assert_equal 101, large_sample.count_segments
    sample.truncate(2)
    assert_equal 2, sample.count_segments
    assert_equal 101, sample.params[:segment_count]
  end
    
  private
  def nested_sample
    t = NewRelic::TransactionSample.new
    
    t.params[:test] = "hi"
    
    s1 = t.create_segment(1.0, "controller")
    
    t.root_segment.add_called_segment(s1)
    
    s2 = t.create_segment(2.0, "AR1")
    
    s2.params[:test] = "test"
    
    s1.add_called_segment(s2)
    s2.end_trace 3.0
    s1.end_trace 4.0
    
    s3 = t.create_segment(4.0, "post_filter")
    t.root_segment.add_called_segment(s3)
    s3.end_trace 6.0
    
    s4 = t.create_segment(6.0, "post_filter")
    t.root_segment.add_called_segment(s4)
    s4.end_trace 7.0
    t
  end
  
  def large_sample
    t = NewRelic::TransactionSample.new
    
    s1 = t.create_segment(1.0, "controller")
    t.root_segment.add_called_segment(s1)
    
    100.times do
      s1.add_called_segment(t.create_segment(1.0, "segment"))
    end
    t
  end
  

end
