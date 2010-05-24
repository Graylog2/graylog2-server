require File.expand_path(File.join(File.dirname(__FILE__),'..', 'test_helper'))
##require "new_relic/stats"
class NewRelic::MetricSpecTest < Test::Unit::TestCase
  
  def test_simple
    stats = NewRelic::MethodTraceStats.new 
    validate stats, 0, 0, 0, 0
    
    assert_equal stats.call_count,0
    stats.trace_call 10
    stats.trace_call 20
    stats.trace_call 30
    
    validate stats, 3, (10+20+30), 10, 30
  end
  
  def test_equal
    spec1 = NewRelic::MetricSpec.new('Controller')
    spec2 = NewRelic::MetricSpec.new('Controller', nil)
    
    assert spec1.eql?(NewRelic::MetricSpec.new('Controller'))
    assert spec2.eql?(NewRelic::MetricSpec.new('Controller', nil))
    assert spec1.eql?(spec2)
    assert !spec2.eql?(NewRelic::MetricSpec.new('Controller', '/dude'))
  end

  define_method(:'test_<=>') do
    s1 = NewRelic::MetricSpec.new('ActiveRecord')
    s2 = NewRelic::MetricSpec.new('Controller')
    assert_equal [s1, s2].sort, [s1,s2]
    assert_equal [s2, s1].sort, [s1,s2]

    s1 = NewRelic::MetricSpec.new('Controller', nil)
    s2 = NewRelic::MetricSpec.new('Controller', 'hap')
    assert_equal [s2, s1].sort, [s1, s2]
    assert_equal [s1, s2].sort, [s1, s2]
    
    s1 = NewRelic::MetricSpec.new('Controller', 'hap')
    s2 = NewRelic::MetricSpec.new('Controller', nil)
    assert_equal [s2, s1].sort, [s2, s1]
    assert_equal [s1, s2].sort, [s2, s1]
    
    s1 = NewRelic::MetricSpec.new('Controller')
    s2 = NewRelic::MetricSpec.new('Controller')
    assert_equal [s2, s1].sort, [s2, s1] # unchanged due to no sort criteria
    assert_equal [s1, s2].sort, [s1, s2] # unchanged due to no sort criteria
    
    s1 = NewRelic::MetricSpec.new('Controller', nil)
    s2 = NewRelic::MetricSpec.new('Controller', nil)
    assert_equal [s2, s1].sort, [s2, s1] # unchanged due to no sort criteria
    assert_equal [s1, s2].sort, [s1, s2] # unchanged due to no sort criteria  
  end
  
  def test_merge
    s1 = NewRelic::MethodTraceStats.new
    s2 = NewRelic::MethodTraceStats.new
    
    s1.trace_call 10
    s2.trace_call 20
    s2.freeze
    
    validate s2, 1, 20, 20, 20
    s3 = s1.merge s2
    validate s3, 2, (10+20), 10, 20
    validate s1, 1, 10, 10, 10
    validate s2, 1, 20, 20, 20
    
    s1.merge! s2
    validate s1, 2, (10+20), 10, 20
    validate s2, 1, 20, 20, 20
  end
  
  def test_merge_with_exclusive
    s1 = NewRelic::MethodTraceStats.new
    
    s2 = NewRelic::MethodTraceStats.new
    
    s1.trace_call 10, 5
    s2.trace_call 20, 10
    s2.freeze
    
    validate s2, 1, 20, 20, 20, 10
    s3 = s1.merge s2
    validate s3, 2, (10+20), 10, 20, (10+5)
    validate s1, 1, 10, 10, 10, 5
    validate s2, 1, 20, 20, 20, 10
    
    s1.merge! s2
    validate s1, 2, (10+20), 10, 20, (5+10)
    validate s2, 1, 20, 20, 20, 10
  end
  
  def test_merge_array
    s1 = NewRelic::MethodTraceStats.new
    merges = []
    merges << (NewRelic::MethodTraceStats.new.trace_call 1)
    merges << (NewRelic::MethodTraceStats.new.trace_call 1)
    merges << (NewRelic::MethodTraceStats.new.trace_call 1)
    
    s1.merge! merges
    validate s1, 3, 3, 1, 1
  end
  
  def test_freeze
    s1 = NewRelic::MethodTraceStats.new
    
    s1.trace_call 10
    s1.freeze
    
    begin
      # the following should throw an exception because s1 is frozen
      s1.trace_call 20
      assert false
    rescue StandardError
      assert s1.frozen?
      validate s1, 1, 10, 10, 10
    end
  end
  
  def test_std_dev
    s = NewRelic::MethodTraceStats.new
    
    s.trace_call 10
    s.trace_call 10
    s.trace_call 10
    s.trace_call 10
    s.trace_call 10
    s.trace_call 10
    assert s.standard_deviation == 0
    
    s = NewRelic::MethodTraceStats.new
    s.trace_call 4
    s.trace_call 7
    s.trace_call 13
    s.trace_call 16
    s.trace_call 8
    s.trace_call 4
    assert_equal(s.sum_of_squares, 4**2 + 7**2 + 13**2 + 16**2 + 8**2 + 4**2)
    
    s.trace_call 9
    s.trace_call 3
    s.trace_call 1000
    s.trace_call 4
    
    # calculated stdev (population, not sample) from a spreadsheet.
    assert_in_delta(s.standard_deviation, 297.76, 0.01)
  end
  
  def test_std_dev_merge
    s1 = NewRelic::MethodTraceStats.new
    s1.trace_call 4
    s1.trace_call 7
    
    s2 = NewRelic::MethodTraceStats.new
    s2.trace_call 13
    s2.trace_call 16
    
    s3 = s1.merge(s2)
    
    assert(s1.sum_of_squares, 4*4 + 7*7)
    assert_in_delta(s1.standard_deviation, 1.5, 0.01)
    
    assert_in_delta(s2.standard_deviation, 1.5, 0.01)
    assert_equal(s3.sum_of_squares, 4*4 + 7*7 + 13*13 + 16*16, "check sum of squares")
    assert_in_delta(s3.standard_deviation, 4.743, 0.01)
  end
  
  private
  def validate (stats, count, total, min, max, exclusive = nil)
    assert_equal stats.call_count, count
    assert_equal stats.total_call_time, total
    assert_equal stats.average_call_time, (count > 0 ? total / count : 0)
    assert_equal stats.min_call_time, min
    assert_equal stats.max_call_time, max
    assert_equal stats.total_exclusive_time, exclusive if exclusive
  end
end
