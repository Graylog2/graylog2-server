require File.expand_path(File.join(File.dirname(__FILE__),'..', 'test_helper'))
##require "new_relic/stats"

module NewRelic; class TestObjectForStats
                   include Stats
                   attr_accessor :total_call_time
                   attr_accessor :total_exclusive_time
                   attr_accessor :begin_time
                   attr_accessor :end_time
                   attr_accessor :call_count
end; end


class NewRelic::StatsTest < Test::Unit::TestCase
  
  def test_simple
    stats = NewRelic::MethodTraceStats.new 
    validate stats, 0, 0, 0, 0
    
    assert_equal stats.call_count,0
    stats.trace_call 10
    stats.trace_call 20
    stats.trace_call 30
    
    validate stats, 3, (10+20+30), 10, 30
  end

  def test_to_s
    s1 = NewRelic::MethodTraceStats.new
    s1.trace_call 10
    assert_equal(s1.to_s, "Begin=0.0, Duration=0.0 s, Count=1, Total=10000, Total Exclusive=10000, Avg=10000, Min=10000, Max=10000, StdDev=0")
  end

  def test_time_str
    s1 = NewRelic::MethodTraceStats.new
    assert_equal(s1.time_str(10), "10 ms")
    assert_equal(s1.time_str(4999), "4999 ms")    
    assert_equal(s1.time_str(5000), "5.00 s")
    assert_equal(s1.time_str(5010), "5.01 s")
    assert_equal(s1.time_str(9999), "10.00 s")
    assert_equal(s1.time_str(10000), "10.0 s")
    assert_equal(s1.time_str(20000), "20.0 s")
  end    

  def test_fraction_of
    s1 = NewRelic::MethodTraceStats.new
    s2 = NewRelic::MethodTraceStats.new
    s1.trace_call 10
    s2.trace_call 20
    assert_equal(s1.fraction_of(s2).to_s, 'NaN')
  end

  def test_fraction_of2
    s1 = NewRelic::MethodTraceStats.new
    s1.trace_call 10
    s2 = NewRelic::MethodTraceStats.new
    assert_equal(s1.fraction_of(s2).to_s, 'NaN')
  end

  def test_multiply_by
    s1 = NewRelic::MethodTraceStats.new
    s1.trace_call 10
    assert_equal(s1.multiply_by(10).to_s, "Begin=0.0, Duration=0.0 s, Count=10, Total=100000, Total Exclusive=10000, Avg=10000, Min=10000, Max=10000, StdDev=0")
  end

  def test_get_apdex
    s1 = NewRelic::MethodTraceStats.new
    s1.trace_call 10
    assert_equal(s1.get_apdex, [1, 10, 10])
  end

  def test_apdex_score
    s1 = NewRelic::MethodTraceStats.new
    s1.trace_call 10
    # FIXME make this test the real logic
    # don't ask me what this means, but it's what's coming out the
    # other end when I actually run it.
    assert_in_delta(s1.apdex_score, 0.285714285714286, 0.0000001)
  end

  def test_as_percentage
    s1 = NewRelic::MethodTraceStats.new
    s1.trace_call 10
    assert_equal(s1.as_percentage, 1000.0)
  end

  def test_calls_per_minute

    s1 = NewRelic::TestObjectForStats.new
    s1.call_count =  1
    s1.begin_time = Time.at(0)
    s1.end_time = Time.at(30)
    assert_equal(s1.calls_per_minute, 2)
  end

  def test_total_call_time_per_minute
    s1 = NewRelic::TestObjectForStats.new
    s1.begin_time = Time.at(0)
    s1.end_time = Time.at(0)
    assert_equal(0, s1.total_call_time_per_minute)
    s1.begin_time = Time.at(0)
    s1.end_time = Time.at(30)
    s1.total_call_time = 10
    assert_equal(20, s1.total_call_time_per_minute)
  end

  def test_time_percentage
    s1 = NewRelic::TestObjectForStats.new
    s1.begin_time = Time.at(0)
    s1.end_time = Time.at(0)
    assert_equal(0, s1.time_percentage)
    s1.total_call_time = 10
    s1.begin_time = Time.at(0)
    s1.end_time = Time.at(30)
    assert_equal((1.0 / 3.0), s1.time_percentage)
    s1.total_call_time = 20
    assert_equal((2.0 / 3.0), s1.time_percentage)
  end

  def test_exclusive_time_percentage
    s1 = NewRelic::TestObjectForStats.new
    s1.begin_time = Time.at(0)
    s1.end_time = Time.at(0)
    assert_equal(0, s1.exclusive_time_percentage)
    s1.total_exclusive_time = 10
    s1.begin_time = Time.at(0)
    s1.end_time = Time.at(30)
    assert_equal((1.0 / 3.0), s1.exclusive_time_percentage)
    s1.total_exclusive_time = 20
    assert_equal((2.0 / 3.0), s1.exclusive_time_percentage)
  end

  def test_sum_merge
    s1 = NewRelic::MethodTraceStats.new
    s2 = NewRelic::MethodTraceStats.new
    s1.trace_call 10
    s2.trace_call 20
    s2.freeze
    
    validate s1, 1, 10, 10, 10
    validate s2, 1, 20, 20, 20
    s1.sum_merge! s2
    validate s1, 1, (10+20), 10 + 20, 20 + 10
    validate s2, 1, 20, 20, 20
  end

  def test_sum_merge_with_exclusive
    s1 = NewRelic::MethodTraceStats.new
    s2 = NewRelic::MethodTraceStats.new

    s1.trace_call 10, 5
    s2.trace_call 20, 10
    s2.freeze

    validate s1, 1, 10, 10, 10, 5
    validate s2, 1, 20, 20, 20, 10
    s1.sum_merge! s2
    validate s1, 1, (10+20), 10 + 20, 20 + 10, (10+5)
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
  def test_round
    stats = NewRelic::MethodTraceStats.new
    stats.record_data_point(0.125222, 0.025)
    stats.record_data_point(0.125222, 0.025)
    stats.record_data_point(0.125222, 0.025)
    assert_equal 0.047041647852, stats.sum_of_squares
    assert_equal 0.375666, stats.total_call_time
    stats.round!
    assert_equal 0.376, stats.total_call_time
    assert_equal 0.047, stats.sum_of_squares
    
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
    s.trace_call 1
    assert s.standard_deviation == 0
    
    s = NewRelic::MethodTraceStats.new
    s.trace_call 10
    s.trace_call 10
    s.sum_of_squares = nil
    assert s.standard_deviation == 0
    
    s = NewRelic::MethodTraceStats.new
    s.trace_call 0.001
    s.trace_call 0.001
    assert s.standard_deviation == 0

    
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
