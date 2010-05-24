# NOTE there are multiple implementations of the MemCache client in Ruby,
# each with slightly different API's and semantics.  
# See:
#     http://www.deveiate.org/code/Ruby-MemCache/ (Gem: Ruby-MemCache)
#     http://seattlerb.rubyforge.org/memcache-client/ (Gem: memcache-client)
unless NewRelic::Control.instance['disable_memcache_instrumentation']
  
  def self.instrument_method(the_class, method_name)
    return unless the_class.method_defined? method_name.to_sym
    the_class.class_eval <<-EOD
        def #{method_name}_with_newrelic_trace(*args)
          metrics = ["MemCache/#{method_name}", 
                     (NewRelic::Agent::Instrumentation::MetricFrame.recording_web_transaction? ? 'MemCache/allWeb' : 'MemCache/allOther')]
          self.class.trace_execution_scoped(metrics) do
            #{method_name}_without_newrelic_trace(*args)
          end
        end
        alias #{method_name}_without_newrelic_trace #{method_name}
        alias #{method_name} #{method_name}_with_newrelic_trace
    EOD
  end
  # Support for libmemcached through Evan Weaver's memcached wrapper
  # http://blog.evanweaver.com/files/doc/fauna/memcached/classes/Memcached.html    
  %w[get get_multi set add incr decr delete replace append prepand cas].each do | method_name |
    instrument_method(::MemCache, method_name) if defined? ::MemCache  
    instrument_method(::Memcached, method_name) if defined? ::Memcached  
  end

end
