module NewRelic
module Agent
  class StatsEngine
        
    # Defines methods that stub out the stats engine methods
    # when the agent is disabled
    
    class ScopeStackElement
      attr_reader :name, :deduct_call_time_from_parent
      attr_accessor :children_time
      def initialize(name, deduct_call_time)
        @name = name
        @deduct_call_time_from_parent = deduct_call_time
        @children_time = 0
      end
    end
    
    module Transactions
      module Shim # :nodoc:
        def start_transaction(*args); end
        def end_transaction; end
        def push_scope(*args); end
        def transaction_sampler=(*args); end
        def scope_name=(*args); end
        def scope_name; end
        def pop_scope(*args); end
      end
      
      def transaction_sampler= sampler
        fail "Can't add a scope listener midflight in a transaction" if scope_stack.any?
        @transaction_sampler = sampler
      end
      
      def remove_transaction_sampler(l)
        @transaction_sampler = nil
      end
      
      def push_scope(metric, time = Time.now.to_f, deduct_call_time_from_parent = true)
        
        stack = scope_stack
        if collecting_gc?
          if stack.empty?
            # reset the gc time so we only include gc time spent during this call
            @last_gc_timestamp = GC.time
            @last_gc_count = GC.collections
          else
            capture_gc_time
          end
        end
        @transaction_sampler.notice_push_scope metric, time if @transaction_sampler
        scope = ScopeStackElement.new(metric, deduct_call_time_from_parent)
        stack.push scope
        scope
      end
      
      def pop_scope(expected_scope, duration, time=Time.now.to_f)
        capture_gc_time if collecting_gc?
        stack = scope_stack
        scope = stack.pop
        fail "unbalanced pop from blame stack, got #{scope ? scope.name : 'nil'}, expected #{expected_scope ? expected_scope.name : 'nil'}" if scope != expected_scope
        
        if !stack.empty? 
          if scope.deduct_call_time_from_parent
            stack.last.children_time += duration
          else
            stack.last.children_time += scope.children_time
          end
        end
        @transaction_sampler.notice_pop_scope(scope.name, time) if @transaction_sampler
        scope
      end
      
      def peek_scope
        scope_stack.last
      end
      
      # set the name of the transaction for the current thread, which will be used
      # to define the scope of all traced methods called on this thread until the
      # scope stack is empty.  
      #
      # currently the transaction name is the name of the controller action that 
      # is invoked via the dispatcher, but conceivably we could use other transaction
      # names in the future if the traced application does more than service http request
      # via controller actions
      def scope_name=(transaction)
        Thread::current[:newrelic_scope_name] = transaction
      end
      
      def scope_name
        Thread::current[:newrelic_scope_name]
      end
      
      # Start a new transaction, unless one is already in progress
      def start_transaction(name = nil)
        Thread::current[:newrelic_scope_stack] ||= []
        self.scope_name = name if name
      end
      
      # Try to clean up gracefully, otherwise we leave things hanging around on thread locals.
      # If it looks like a transaction is still in progress, then maybe this is an inner transaction
      # and is ignored.
      #
      def end_transaction
        stack = scope_stack
        
        if stack && stack.empty?
          Thread::current[:newrelic_scope_stack] = nil
          Thread::current[:newrelic_scope_name] = nil
        end
      end
      
      private
      
      # Make sure we don't do this in a multi-threaded environment
      def collecting_gc?
        @@collecting_gc ||= GC.respond_to?(:time) && GC.respond_to?(:collections) && !NewRelic::Control.instance.multi_threaded?  
      end
      
      # Assumes collecting_gc?
      def capture_gc_time
        # Skip this if we are already in this segment
        return if !scope_stack.empty? && scope_stack.last.name == "GC/cumulative"
        num_calls = GC.collections - @last_gc_count
        elapsed = (GC.time - @last_gc_timestamp).to_f
        @last_gc_timestamp = GC.time
        @last_gc_count = GC.collections
        if num_calls > 0
          # µs to seconds
          elapsed = elapsed / 1000000.0
          # Allocate the GC time to a scope as if the GC just ended
          # right now.
          time = Time.now.to_f
          gc_scope = push_scope("GC/cumulative", time - elapsed)
          # GC stats are collected into a blamed metric which allows
          # us to show the stats controller by controller
          gc_stats = NewRelic::Agent.get_stats(gc_scope.name, true)  
          gc_stats.record_multiple_data_points(elapsed, num_calls)
          pop_scope(gc_scope, elapsed, time)
        end
      end
      
      def scope_stack
        Thread::current[:newrelic_scope_stack] ||= []
      end
      
    end
  end
end
end
