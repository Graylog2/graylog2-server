
module NewRelic
  module Stats
    
    def absent?
      # guess on absent values
      call_count == 0
    end  

    def time_str(value_ms)
      case
        when value_ms >= 10000 
       "%.1f s" % (value_ms / 1000.0)
        when value_ms >= 5000 
       "%.2f s" % (value_ms / 1000.0)
      else
       "%.0f ms" % value_ms
      end
    end
    
    def average_call_time
      return 0 if call_count == 0
      total_call_time / call_count
    end
    def average_exclusive_time
      return 0 if call_count == 0
      total_exclusive_time / call_count
    end

    # merge by adding to average response time
    # - used to compose multiple metrics e.g. dispatcher time + mongrel queue time
    def sum_merge! (other_stats)
      Array(other_stats).each do |s|
        self.total_call_time += s.total_call_time
        self.total_exclusive_time += s.total_exclusive_time
        self.min_call_time += s.min_call_time
        self.max_call_time += s.max_call_time
        #self.call_count += s.call_count - do not add call count because we are stacking these times on top of each other
        self.sum_of_squares += s.sum_of_squares if s.sum_of_squares
        self.begin_time = s.begin_time if s.begin_time.to_f < begin_time.to_f || begin_time.to_f == 0.0
        self.end_time = s.end_time if s.end_time.to_f > end_time.to_f
      end

      self
    end

    def merge! (other_stats)
      Array(other_stats).each do |s|
        self.total_call_time += s.total_call_time
        self.total_exclusive_time += s.total_exclusive_time
        self.min_call_time = s.min_call_time if (s.min_call_time < min_call_time && s.call_count > 0) || call_count == 0
        self.max_call_time = s.max_call_time if s.max_call_time > max_call_time
        self.call_count += s.call_count
        self.sum_of_squares += s.sum_of_squares if s.sum_of_squares
        self.begin_time = s.begin_time if s.begin_time.to_f < begin_time.to_f || begin_time.to_f == 0.0
        self.end_time = s.end_time if s.end_time.to_f > end_time.to_f
      end
      
      self
    end
    
    def merge (other_stats)
      stats = self.clone
      stats.merge! other_stats
    end
    
    # split into an array of timeslices whose
    # time boundaries start on (begin_time + (n * duration)) and whose
    # end time ends on (begin_time * (n + 1) * duration), except for the
    # first and last elements, whose begin time and end time are the begin
    # and end times of this stats instance, respectively.  Yield to caller
    # for the code that creates the actual stats instance
    def split(rollup_begin_time, rollup_period)
      rollup_begin_time = rollup_begin_time.to_f
      rollup_begin_time += ((self.begin_time - rollup_begin_time) / rollup_period).floor * rollup_period

      current_begin_time = self.begin_time
      current_end_time = rollup_begin_time + rollup_period

      return [self] if current_end_time >= self.end_time
      
      timeslices = []
      while current_end_time < self.end_time do
        ts = yield(current_begin_time, current_end_time)
        if ts
          ts.fraction_of(self)
          timeslices << ts
        end
        current_begin_time = current_end_time
        current_end_time = current_begin_time + rollup_period
      end
      
      if self.end_time > current_begin_time
        percentage = rollup_period / self.duration + (self.begin_time - rollup_begin_time) / rollup_period
        ts = yield(current_begin_time, self.end_time)
        if ts
          ts.fraction_of(self)
          timeslices << ts
        end
      end
      
      timeslices
    end
    
    def is_reset?
      call_count == 0 && total_call_time == 0.0 && total_exclusive_time == 0.0
    end
    
    def reset
      self.call_count = 0
      self.total_call_time = 0.0
      self.total_exclusive_time = 0.0
      self.min_call_time = 0.0
      self.max_call_time = 0.0
      self.sum_of_squares = 0.0
      self.begin_time = Time.at(0)
      self.end_time = Time.at(0)
    end
    
    def as_percentage_of(other_stats)
      return 0 if other_stats.total_call_time == 0
      return (total_call_time / other_stats.total_call_time) * 100.0
    end
    
    # the stat total_call_time is a percent
    def as_percentage
      if call_count.zero?
        0
      else
        (total_call_time / call_count) * 100.0
      end
    end
    
    def duration
      end_time - begin_time
    end

    def calls_per_minute
      if duration.zero?
        0
      else
        (call_count / duration.to_f) * 60.0
      end
    end    
    
    def total_call_time_per_minute
      60.0 * time_percentage
    end
    
    def standard_deviation
      return 0 if call_count < 2 || self.sum_of_squares.nil?
      
      # Convert sum of squares into standard deviation based on
      # formula for the standard deviation for the entire population
      x = self.sum_of_squares - (self.call_count * (self.average_value**2))
      return 0 if x <= 0
      
      Math.sqrt(x / self.call_count)
    end
    
    # returns the time spent in this component as a percentage of the total
    # time window.
    def time_percentage
      return 0 if duration == 0
      total_call_time / duration
    end

    def exclusive_time_percentage
      return 0 if duration == 0
      total_exclusive_time / duration
    end

    alias average_value average_call_time
    alias average_response_time average_call_time
    alias requests_per_minute calls_per_minute
    
    def to_s
      s = "Begin=#{begin_time}, "
      s << "Duration=#{duration} s, "
      s << "Count=#{call_count}, "
      s << "Total=#{to_ms(total_call_time)}, "
      s << "Total Exclusive=#{to_ms(total_exclusive_time)}, "
      s << "Avg=#{to_ms(average_call_time)}, "
      s << "Min=#{to_ms(min_call_time)}, "
      s << "Max=#{to_ms(max_call_time)}, "
      s << "StdDev=#{to_ms(standard_deviation)}"
    end
    
    # Summary string to facilitate testing
    def summary
      format = "%m/%d %I:%M%p"
      "[#{Time.at(begin_time).strftime(format)}, #{'%2.3fs' % duration}; #{'%4i' % call_count} calls #{'%4i' % to_ms(average_call_time)} ms]"
    end
    
    # round all of the values to n decimal points
    def round!
      self.total_call_time = round_to_3(total_call_time)
      self.total_exclusive_time = round_to_3(total_exclusive_time)
      self.min_call_time = round_to_3(min_call_time)
      self.max_call_time = round_to_3(max_call_time)
      self.sum_of_squares = round_to_3(sum_of_squares)
      self.begin_time = begin_time
      self.end_time = end_time
    end

    # calculate this set of stats to be a percentage fraction 
    # of the provided stats, which has an overlapping time window.
    # used as a key part of the split algorithm
    def fraction_of(s)
      min_end = (end_time < s.end_time ? end_time : s.end_time)
      max_begin = (begin_time > s.begin_time ? begin_time : s.begin_time)
      percentage = (min_end - max_begin) / s.duration

      self.total_exclusive_time = s.total_exclusive_time * percentage
      self.total_call_time = s.total_call_time * percentage
      self.min_call_time = s.min_call_time
      self.max_call_time = s.max_call_time
      self.call_count = s.call_count * percentage
      self.sum_of_squares = (s.sum_of_squares || 0) * percentage
    end
    
    # multiply the total time and rate by the given percentage 
    def multiply_by(percentage)
      self.total_call_time = total_call_time * percentage
      self.call_count = call_count * percentage
      self.sum_of_squares = sum_of_squares * percentage
      
      self
    end
    

    # returns s,t,f
    def get_apdex
      [@call_count, @total_call_time.to_i, @total_exclusive_time.to_i]
    end

    def apdex_score
      s, t, f = get_apdex
      (s.to_f + (t.to_f / 2)) / (s+t+f).to_f
    end

    private
    
    def to_ms(number)
      (number*1000).round
    end
        
    def round_to_3(val)
      (val * 1000).round / 1000.0
    end
  end
  
  
  class StatsBase
    include Stats

    attr_accessor :call_count
    attr_accessor :min_call_time
    attr_accessor :max_call_time
    attr_accessor :total_call_time
    attr_accessor :total_exclusive_time
    attr_accessor :sum_of_squares
    
    def initialize 
      reset
    end
    
    def freeze
      @end_time = Time.now
      super
    end
    
    def to_json(*a)
      {'call_count' => call_count, 
      'min_call_time' => min_call_time, 
      'max_call_time' => max_call_time, 
      'total_call_time' => total_call_time,
      'total_exclusive_time' => total_exclusive_time,
      'sum_of_squares' => sum_of_squares}.to_json(*a)
    end

    
    # In this class, we explicitly don't track begin and end time here, to save space during
    # cross process serialization via xml.  Still the accessor methods must be provided for merge to work.
    def begin_time=(t)
    end
    
    def end_time=(t)
    end
    
    def begin_time
      0.0
    end
    
    def end_time
      0.0
    end
  end
  
  
  class BasicStats < StatsBase
  end
  
  class ApdexStats < StatsBase
    
    def record_apdex_s
      @call_count += 1
    end
    
    def record_apdex_t
      @total_call_time += 1
    end
    
    def record_apdex_f
      @total_exclusive_time += 1
    end
  end
  
  # Statistics used to track the performance of traced methods
  class MethodTraceStats < StatsBase
    
    alias data_point_count call_count
    
    # record a single data point into the statistical gatherer.  The gatherer
    # will aggregate all data points collected over a specified period and upload
    # its data to the NewRelic server
    def record_data_point(value, exclusive_time = value)
      @call_count += 1
      @total_call_time += value
      @min_call_time = value if value < @min_call_time || @call_count == 1
      @max_call_time = value if value > @max_call_time
      @total_exclusive_time += exclusive_time

      @sum_of_squares += (value * value)
      self
    end
    
    alias trace_call record_data_point
    
    def record_multiple_data_points(total_value, count=1)
      return record_data_point(total_value) if count == 1
      @call_count += count
      @total_call_time += total_value
      avg_val = total_value / count
      @min_call_time = avg_val if avg_val < @min_call_time || @call_count == count
      @max_call_time = avg_val if avg_val > @max_call_time
      @total_exclusive_time += total_value
      @sum_of_squares += (avg_val * avg_val) * count
      self
    end
    
    def increment_count(value = 1)
      @call_count += value
    end

  end
  
  class ScopedMethodTraceStats < MethodTraceStats
    def initialize(unscoped_stats)
      super()
      @unscoped_stats = unscoped_stats
    end
    def trace_call(call_time, exclusive_time = call_time)
      @unscoped_stats.trace_call call_time, exclusive_time
      super call_time, exclusive_time
    end
    def record_multiple_data_points(total_value, count=1)
      @unscoped_stats.record_multiple_data_points(total_value, count)
      super total_value, count
    end
    def unscoped_stats
      @unscoped_stats
    end
  end
end

