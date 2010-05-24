module NewRelic
module Agent
  class StatsEngine
    module MetricStats
      # The stats hash hashes either a metric name for an unscoped metric,
      # or a metric_spec for a scoped metric value.
      def lookup_stat(metric_name)
        stats_hash[metric_name]
      end

      def metrics
        stats_hash.keys.map(&:to_s)
      end
      
      def get_stats_no_scope(metric_name)
        stats_hash[metric_name] ||= NewRelic::MethodTraceStats.new 
      end
      
      # This version allows a caller to pass a stat class to use
      #
      def get_custom_stats(metric_name, stat_class)
        stats_hash[metric_name] ||= stat_class.new
      end
      
      # If use_scope is true, two chained metrics are created, one with scope and one without
      # If scoped_metric_only is true, only a scoped metric is created (used by rendering metrics which by definition are per controller only)
      def get_stats(metric_name, use_scope = true, scoped_metric_only = false)
        
        if scoped_metric_only
          spec = NewRelic::MetricSpec.new metric_name, scope_name
          stats = stats_hash[spec] ||= NewRelic::MethodTraceStats.new 
        else  
          stats = stats_hash[metric_name] ||= NewRelic::MethodTraceStats.new 
          if use_scope && scope_name && scope_name != metric_name 
            spec = NewRelic::MetricSpec.new metric_name, scope_name
            scoped_stats = stats_hash[spec] ||= NewRelic::ScopedMethodTraceStats.new(stats) 
            stats = scoped_stats
          end
        end
        stats
      end
      
      def lookup_stats(metric_name, scope_name = nil)
        stats_hash[NewRelic::MetricSpec.new(metric_name, scope_name)] ||
        stats_hash[metric_name]
      end
      # Harvest the timeslice data.  First recombine current statss
      # with any previously
      # unsent metrics, clear out stats cache, and return the current
      # stats. 
      # ---
      # Note: this is not synchronized.  There is still some risk in this and
      # we will revisit later to see if we can make this more robust without
      # sacrificing efficiency.
      # +++
      def harvest_timeslice_data(previous_timeslice_data, metric_ids)
        timeslice_data = {}
        poll harvest_samplers
        stats_hash.keys.each do | metric_spec |
          
          
          # get a copy of the stats collected since the last harvest, and clear
          # the stats inside our hash table for the next time slice.
          stats = stats_hash[metric_spec]
          
          # we have an optimization for unscoped metrics
          if !(metric_spec.is_a? NewRelic::MetricSpec)
            metric_spec = NewRelic::MetricSpec.new metric_spec
          end
          
          if stats.nil? 
            raise "Nil stats for #{metric_spec.name} (#{metric_spec.scope})"
          end
          
          stats_copy = stats.clone
          stats.reset
          
          # if the previous timeslice data has not been reported (due to an error of some sort)
          # then we need to merge this timeslice with the previously accumulated - but not sent
          # data
          previous_metric_data = previous_timeslice_data[metric_spec]
          stats_copy.merge! previous_metric_data.stats unless previous_metric_data.nil?
          stats_copy.round!
          
          # don't bother collecting and reporting stats that have zero-values for this timeslice.
          # significant performance boost and storage savings.
          unless stats_copy.is_reset?

            id = metric_ids[metric_spec]
            metric_spec_for_transport = id ? nil : metric_spec 
            
            metric_data = NewRelic::MetricData.new(metric_spec_for_transport, stats_copy, id)
            
            timeslice_data[metric_spec] = metric_data
          end
        end
        
        timeslice_data
      end
      
      # Remove all stats.  For test code only.
      def clear_stats 
        stats_hash.clear
        NewRelic::Agent::BusyCalculator.reset
      end
      
      # Reset each of the stats, such as when a new passenger instance starts up.
      def reset_stats 
        stats_hash.values.each { |s| s.reset }
      end
      
      def stats_hash
        @stats_hash ||= {}
      end
    end
  end
end
end
