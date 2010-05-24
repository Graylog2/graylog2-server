# Histogram is used for organizing response times
# into an 'Exponential Histogram'.  Feature based in part on DHH proposal:
# http://37signals.com/svn/posts/1836-the-problem-with-averages
#
# Histogram builds a set of buckets of geometrically growing size, with the assumption that most
# apps have long-tail response times, and therefore you need the most granularity at the lowest
# r/t level.
class NewRelic::Histogram

  # Used to stub out API methods when the agent is not enabled
  module Shim
    def process(response_time); end
  end
  # Stores statistics for response times falling in a particular range.
  # A bucket has a min and max response time.  A response time event
  # falls in a bucket if min <= r/t < max.  A bucket also 
  # has an associated metric for reporting data to RPM.  The 
  # bucket range is encoded in the metic name
  class Bucket
    
    attr_reader :min, :max, :stats

    def initialize(min, max = nil)
      @min = min
      @max = max
    end
    
    def stats
      NewRelic::Agent.get_stats("Response Times/#{min_millis}/#{max_millis}")
    end
  
    # This has return value like <=> but does something more
    # than simply compare.  If the value falls within range for
    # the bucket, increment count and return 0; otherwise return
    # a value < 0 if the value belongs in a bucket with a lower range
    # you can guess what it returns if the value belongs in a bucket
    # with a higher range. (Hint: it's not 0, and it's not less than zero.)
    def process(value)
      if value < min
        return -1
    
      # max == nil means unlimited max (last bucket)
      elsif max && value >= max
        return 1

      else
        stats.record_data_point(value)
        return 0
      end
    end

    def min_millis
      (min * 1000).round
    end
  
    def max_millis
      max.nil? ? nil : (max * 1000).round
    end
  
    def to_s
      "#{min_millis} - #{max_millis}: #{stats.call_count}"
    end
  end

  attr_reader :buckets
  
  # Histogram uses apdex T / 10 as its minimum bucket size, and grows from there.
  # 30 data points should be adequate resolution.
  def initialize(first_bucket_max = 0.010, bucket_count = 30, multiplier = 1.3)
    @buckets = []
    
    min = 0
    max = first_bucket_max
    
     (bucket_count - 1).times do 
      @buckets << Bucket.new(min, max)
      min = max
      max *= multiplier
    end
    @buckets << Bucket.new(max)
  end
  
  def process(response_time)
    buckets.each do |bucket|
      found = bucket.process(response_time) 
      return if found == 0
    end
  end
end
