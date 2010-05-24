# This module supports calculation of actual time spent processing requests over the course of
# one harvest period.  It's similar to what you would get if you just added up all the
# execution times of controller calls, however that will be inaccurate when requests
# span the minute boundaries.  This module manages accounting of requests not yet
# completed. 
#
# Calls are re-entrant.  All start calls must be paired with finish
# calls, or a reset call.
module NewRelic
  module Agent
    module BusyCalculator
  
  extend self
  
  # For testability, add accessors:
  attr_reader :harvest_start, :accumulator
  
  def dispatcher_start(time)
    Thread.current[:busy_entries] ||= 0 
    callers = Thread.current[:busy_entries] += 1
    return if callers > 1
    @lock.synchronize do
      @entrypoint_stack.push time      
    end
  end
  
  def dispatcher_finish(end_time = Time.now.to_f)
    callers = Thread.current[:busy_entries] -= 1
    # Ignore nested calls
    return if callers > 0
    @lock.synchronize do
      if @entrypoint_stack.empty?
        NewRelic::Agent.logger.error("Stack underflow tracking dispatcher entry and exit!\n  #{caller.join("  \n")}") 
      else
        @accumulator += (end_time - @entrypoint_stack.pop)
      end
    end
  end
  
  def busy_count
    @entrypoint_stack.size
  end
  
  # Reset the state of the information accumulated by all threads,
  # but only reset the recursion counter for this thread.
  def reset
    @entrypoint_stack = []
    Thread.current[:busy_entries] = 0
    @lock ||= Mutex.new
    @accumulator = 0
    @harvest_start = Time.now.to_f
  end
  
  self.reset
  
  # Called before uploading to to the server to collect current busy stats.
  def harvest_busy
    busy = 0
    t0 = Time.now.to_f
    @lock.synchronize do
      busy = accumulator
      @accumulator = 0
      
      # Walk through the stack and capture all times up to 
      # now for entrypoints
      @entrypoint_stack.size.times do |frame| 
        busy += (t0 - @entrypoint_stack[frame])
        @entrypoint_stack[frame] = t0
      end
      
    end
    
    busy = 0.0 if busy < 0.0 # don't go below 0%
    
    time_window = (t0 - harvest_start)
    time_window = 1.0 if time_window == 0.0  # protect against divide by zero
    
    busy = busy / time_window
    
    instance_busy_stats.record_data_point busy
    @harvest_start = t0
  end
  private
  def instance_busy_stats
    # Late binding on the Instance/busy stats
    NewRelic::Agent.agent.stats_engine.get_stats_no_scope 'Instance/Busy'  
  end
  
end
end
end
