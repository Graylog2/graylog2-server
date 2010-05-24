require 'new_relic/control'
module NewRelic
module Agent
  # This module contains class methods added to support installing custom
  # metric tracers and executing for individual metrics.
  #
  # == Examples
  #
  # When the agent initializes, it extends Module with these methods.
  # However if you want to use the API in code that might get loaded
  # before the agent is initialized you will need to require
  # this file:
  #
  #     require 'new_relic/agent/method_tracer'
  #     class A
  #       include NewRelic::Agent::MethodTracer
  #       def process
  #         ...
  #       end
  #       add_method_tracer :process
  #     end
  #
  # To instrument a class method:
  #
  #     require 'new_relic/agent/method_tracer'
  #     class An
  #       def self.process
  #         ...
  #       end
  #       class << self
  #         include NewRelic::Agent::MethodTracer
  #         add_method_tracer :process
  #       end
  #     end

  module MethodTracer
    
    def self.included clazz #:nodoc:
      clazz.extend ClassMethods
      clazz.send :include, InstanceMethods
    end

    def self.extended clazz #:nodoc:
      clazz.extend ClassMethods
      clazz.extend InstanceMethods
    end

    module InstanceMethods
      # Deprecated: original method preserved for API backward compatibility.
      # Use either #trace_execution_scoped or #trace_execution_unscoped
      def trace_method_execution(metric_names, push_scope, produce_metric, deduct_call_time_from_parent, &block) #:nodoc: 
        if push_scope
          trace_execution_scoped(metric_names, :metric => produce_metric, 
                                                    :deduct_call_time_from_parent => deduct_call_time_from_parent, &block)
        else
          trace_execution_unscoped(metric_names, &block)
        end
      end
      
      # Trace a given block with stats assigned to the given metric_name.  It does not 
      # provide scoped measurements, meaning whatever is being traced will not 'blame the
      # Controller'--that is to say appear in the breakdown chart. 
      # This is code is inlined in #add_method_tracer.
      # * <tt>metric_names</tt> is a single name or an array of names of metrics
      # * <tt>:force => true</tt> will force the metric to be captured even when 
      #   tracing is disabled with NewRelic::Agent#disable_all_tracing
      #
      def trace_execution_unscoped(metric_names, options={})
        return yield unless NewRelic::Agent.is_execution_traced?
        t0 = Time.now.to_f
        stats = Array(metric_names).map do | metric_name |
          NewRelic::Agent.instance.stats_engine.get_stats_no_scope metric_name
        end
        begin
          NewRelic::Agent.instance.push_trace_execution_flag(true) if options[:force]
          yield
        ensure
          NewRelic::Agent.instance.pop_trace_execution_flag if options[:force]
          duration = Time.now.to_f - t0              # for some reason this is 3 usec faster than Time - Time
          stats.each { |stat| stat.trace_call(duration) }
        end
      end
      
      EMPTY_ARRAY = [].freeze
      
      # Deprecated. Use #trace_execution_scoped, a version with an options hash.  
      def trace_method_execution_with_scope(metric_names, produce_metric, deduct_call_time_from_parent, scoped_metric_only=false, &block) #:nodoc:
        trace_execution_scoped(metric_names, 
                             :metric => produce_metric, 
                             :deduct_call_time_from_parent => deduct_call_time_from_parent, 
                             :scoped_metric_only => scoped_metric_only, &block)
      end
      
      alias trace_method_execution_no_scope trace_execution_unscoped #:nodoc:
      
      # Trace a given block with stats and keep track of the caller.  
      # See NewRelic::Agent::MethodTracer::ClassMethods#add_method_tracer for a description of the arguments.
      # +metric_names+ is either a single name or an array of metric names.
      # If more than one metric is passed, the +produce_metric+ option only applies to the first.  The
      # others are always recorded.  Only the first metric is pushed onto the scope stack.
      #
      # Generally you pass an array of metric names if you want to record the metric under additional
      # categories, but generally this *should never ever be done*.  Most of the time you can aggregate
      # on the server.
      
      def trace_execution_scoped(metric_names, options={})
        
        return yield unless NewRelic::Agent.is_execution_traced? || options[:force]
        
        produce_metric               = options[:metric] != false
        deduct_call_time_from_parent = options[:deduct_call_time_from_parent] != false
        scoped_metric_only           = produce_metric && options[:scoped_metric_only]
        t0 = Time.now.to_f
        if metric_names.instance_of? Array
          first_name = metric_names.first
          metric_stats = []
          metric_stats << NewRelic::Agent.instance.stats_engine.get_stats(first_name, true, scoped_metric_only) if produce_metric
          metric_names[1..-1].each do | name |
            metric_stats << NewRelic::Agent.instance.stats_engine.get_stats_no_scope(name) 
          end
        else
          first_name = metric_names      
          if produce_metric
            metric_stats = [NewRelic::Agent.instance.stats_engine.get_stats(first_name, true, scoped_metric_only)]
          else
            metric_stats = EMPTY_ARRAY
          end
        end
        
        begin
          # Keep a reference to the scope we are pushing so we can do a sanity check making
          # sure when we pop we get the one we 'expected'
          NewRelic::Agent.instance.push_trace_execution_flag(true) if options[:force] 
          expected_scope = NewRelic::Agent.instance.stats_engine.push_scope(first_name, t0, deduct_call_time_from_parent)
        rescue => e
          NewRelic::Control.instance.log.error("Caught exception in trace_method_execution header. Metric name = #{first_name}, exception = #{e}")
          NewRelic::Control.instance.log.error(e.backtrace.join("\n"))
        end
        
        begin
          yield
        ensure
          t1 = Time.now.to_f
          duration = t1 - t0
          
          begin
            NewRelic::Agent.instance.pop_trace_execution_flag if options[:force]
            if expected_scope
              scope = NewRelic::Agent.instance.stats_engine.pop_scope expected_scope, duration, t1
              exclusive = duration - scope.children_time
              metric_stats.each { |stats| stats.trace_call(duration, exclusive) }
            end
          rescue => e
            NewRelic::Control.instance.log.error("Caught exception in trace_method_execution footer. Metric name = #{first_name}, exception = #{e}")
            NewRelic::Control.instance.log.error(e.backtrace.join("\n"))
          end
        end
      end
    end
    
    module ClassMethods
      # Add a method tracer to the specified method.
      #
      # === Common Options
      #
      # * <tt>:push_scope => false</tt> specifies this method tracer should not 
      #   keep track of the caller; it will not show up in controller breakdown
      #   pie charts. 
      # * <tt>:metric => false</tt> specifies that no metric will be recorded.
      #   Instead the call will show up in transaction traces as well as traces
      #   shown in Developer Mode. 
      # 
      # === Uncommon Options
      #
      # * <tt>:scoped_metric_only => true</tt> indicates that the unscoped metric
      #   should not be recorded.  Normally two metrics are potentially created
      #   on every invocation: the aggregate method where statistics for all calls
      #   of that metric are stored, and the "scoped metric" which records the
      #   statistics for invocations in a particular scope--generally a controller
      #   action.  This option indicates that only the second type should be recorded.
      #   The effect is similar to <tt>:metric => false</tt> but in addition you
      #   will also see the invocation in breakdown pie charts.
      # * <tt>:deduct_call_time_from_parent => false</tt> indicates that the method invocation
      #   time should never be deducted from the time reported as 'exclusive' in the 
      #   caller.  You would want to use this if you are tracing a recursive method
      #   or a method that might be called inside another traced method.
      # * <tt>:code_header</tt> and <tt>:code_footer</tt> specify ruby code that 
      #   is inserted into the tracer before and after the call.
      # * <tt>:force = true</tt> will ensure the metric is captured even if called inside
      #   an untraced execution call.  (See NewRelic::Agent#disable_all_tracing)
      #
      # === Overriding the metric name
      #
      # +metric_name_code+ is a string that is eval'd to get the 
      # name of the metric associated with the call, so if you want to 
      # use interpolaion evaluated at call time, then single quote
      # the value like this:
      #
      #     add_method_tracer :foo, 'Custom/#{self.class.name}/foo'
      #
      # This would name the metric according to the class of the runtime
      # intance, as opposed to the class where +foo+ is defined.
      #
      # If not provided, the metric name will be <tt>Custom/ClassName/method_name</tt>.
      #
      # === Examples
      #
      # Instrument +foo+ only for custom views--will not show up in transaction traces or caller breakdown graphs:
      #
      #     add_method_tracer :foo, :push_scope => false
      #
      # Instrument +foo+ just for transaction traces only:
      #
      #     add_method_tracer :foo, :metric => false
      #
      # Instrument +foo+ so it shows up in transaction traces and caller breakdown graphs
      # for actions:
      #
      #     add_method_tracer :foo
      # 
      # which is equivalent to:
      #
      #     add_method_tracer :foo, 'Custom/#{self.class.name}/foo', :push_scope => true, :metric => true
      #
      # Instrument the class method +foo+ with the metric name 'Custom/People/fetch':
      # 
      #     class << self
      #       add_method_tracer :foo, 'Custom/People/fetch'
      #     end
      #
      
      def add_method_tracer(method_name, metric_name_code=nil, options = {})
        # for backward compatibility:
        if !options.is_a?(Hash)
          options = {:push_scope => options} 
        end
        # in case they omit the metric name code
        if metric_name_code.is_a?(Hash)
          options.merge(metric_name_code)
        end
        if (unrecognized = options.keys - [:force, :metric, :push_scope, :deduct_call_time_from_parent, :code_header, :code_footer, :scoped_metric_only]).any?
          fail "Unrecognized options in add_method_tracer_call: #{unrecognized.join(', ')}"
        end
        # options[:push_scope] true if we are noting the scope of this for
        # stats collection as well as the transaction tracing
        options[:push_scope] = true if options[:push_scope].nil?
        # options[:metric] true if you are tracking stats for a metric, otherwise
        # it's just for transaction tracing.
        options[:metric] = true if options[:metric].nil?
        options[:force] = false if options[:force].nil?
        options[:deduct_call_time_from_parent] = false if options[:deduct_call_time_from_parent].nil? && !options[:metric]
        options[:deduct_call_time_from_parent] = true if options[:deduct_call_time_from_parent].nil?
        options[:code_header] ||= ""
        options[:code_footer] ||= ""
        options[:scoped_metric_only] ||= false
        
        klass = (self === Module) ? "self" : "self.class"
        # Default to the class where the method is defined.
        metric_name_code = "Custom/#{self.name}/#{method_name.to_s}" unless metric_name_code
        
        unless method_defined?(method_name) || private_method_defined?(method_name)
          NewRelic::Control.instance.log.warn("Did not trace #{self.name}##{method_name} because that method does not exist")
          return
        end
        
        traced_method_name = _traced_method_name(method_name, metric_name_code)
        if method_defined? traced_method_name
          NewRelic::Control.instance.log.warn("Attempt to trace a method twice with the same metric: Method = #{method_name}, Metric Name = #{metric_name_code}")
          return
        end
        
        fail "Can't add a tracer where push_scope is false and metric is false" if options[:push_scope] == false && !options[:metric]
        
        header = ""
        if !options[:force]
          header << "return #{_untraced_method_name(method_name, metric_name_code)}(*args, &block) unless NewRelic::Agent.is_execution_traced?\n"
        end
        header << options[:code_header] if options[:code_header]
        if options[:push_scope] == false
          code = <<-CODE
        def #{_traced_method_name(method_name, metric_name_code)}(*args, &block)
          #{header}
          t0 = Time.now.to_f
          stats = NewRelic::Agent.instance.stats_engine.get_stats_no_scope "#{metric_name_code}"
          begin
            #{"NewRelic::Agent.instance.push_trace_execution_flag(true)\n" if options[:force]}
            #{_untraced_method_name(method_name, metric_name_code)}(*args, &block)\n
          ensure
            #{"NewRelic::Agent.instance.pop_trace_execution_flag\n" if options[:force] }
            duration = Time.now.to_f - t0
            stats.trace_call(duration)
            #{options[:code_footer]}
          end
        end
      CODE
        else
          code = <<-CODE
      def #{_traced_method_name(method_name, metric_name_code)}(*args, &block)
        #{options[:code_header]}
        result = #{klass}.trace_execution_scoped("#{metric_name_code}", 
                  :metric => #{options[:metric]},
                  :forced => #{options[:force]}, 
                  :deduct_call_time_from_parent => #{options[:deduct_call_time_from_parent]}, 
                  :scoped_metric_only => #{options[:scoped_metric_only]}) do
          #{_untraced_method_name(method_name, metric_name_code)}(*args, &block)
        end
        #{options[:code_footer]}
        result
      end
      CODE
        end
        class_eval code, __FILE__, __LINE__
        
        alias_method _untraced_method_name(method_name, metric_name_code), method_name
        alias_method method_name, _traced_method_name(method_name, metric_name_code)
        
        NewRelic::Control.instance.log.debug("Traced method: class = #{self.name}, method = #{method_name}, "+
        "metric = '#{metric_name_code}'")
      end
      
      # For tests only because tracers must be removed in reverse-order
      # from when they were added, or else other tracers that were added to the same method
      # may get removed as well.
      def remove_method_tracer(method_name, metric_name_code) # :nodoc:
        return unless NewRelic::Control.instance.agent_enabled?
        
        if method_defined? "#{_traced_method_name(method_name, metric_name_code)}"
          alias_method method_name, "#{_untraced_method_name(method_name, metric_name_code)}"
          undef_method "#{_traced_method_name(method_name, metric_name_code)}"
        else
          raise "No tracer for '#{metric_name_code}' on method '#{method_name}'"
        end
      end
      private
      
      def _untraced_method_name(method_name, metric_name)
    "#{_sanitize_name(method_name)}_without_trace_#{_sanitize_name(metric_name)}" 
      end
      
      def _traced_method_name(method_name, metric_name)
    "#{_sanitize_name(method_name)}_with_trace_#{_sanitize_name(metric_name)}" 
      end
      
      def _sanitize_name(name)
        name.to_s.tr_s('^a-zA-Z0-9', '_')
      end
    end
  end
end
end
