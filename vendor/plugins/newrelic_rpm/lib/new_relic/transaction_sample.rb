
module NewRelic
  COLLAPSE_SEGMENTS_THRESHOLD = 2
  
  MYSQL_EXPLAIN_COLUMNS = [
        "Id",
        "Select Type",
        "Table",
        "Type",
        "Possible Keys",
        "Key",
        "Key Length",
        "Ref",
        "Rows",
        "Extra"
      ].freeze
  
  class TransactionSample
    EMPTY_ARRAY = [].freeze
    
    @@start_time = Time.now

    include TransactionAnalysis
    class Segment
      
      
      attr_reader :entry_timestamp
      # The exit timestamp will be relative except for the outermost sample which will 
      # have a timestamp.
      attr_reader :exit_timestamp
      attr_reader :parent_segment
      attr_reader :metric_name
      attr_reader :segment_id
      
      def initialize(timestamp, metric_name, segment_id)
        @entry_timestamp = timestamp
        @metric_name = metric_name || '<unknown>'
        @segment_id = segment_id || object_id
      end
      
      def end_trace(timestamp)
        @exit_timestamp = timestamp
      end
      
      def add_called_segment(s)
        @called_segments ||= []
        @called_segments << s
        s.parent_segment = self
      end
      
      def to_s
        to_debug_str(0)
      end
      
      def to_json
        map = {:entry_timestamp => @entry_timestamp,
          :exit_timestamp => @exit_timestamp,
          :metric_name => @metric_name,
          :segment_id => @segment_id}
        if @called_segments && !@called_segments.empty?
          map[:called_segments] = @called_segments
        end
        if @params && !@params.empty?
          map[:params] = @params
        end
        map.to_json
      end
      
      def self.from_json(json, id_generator)
        json = ActiveSupport::JSON.decode(json) if json.is_a?(String)
        if json.is_a?(Array)
          entry_timestamp = json[0].to_f / 1000
          exit_timestamp = json[1].to_f / 1000
          metric_name = json[2]
          params = json[3]
          called_segments = json[4]
        else
          entry_timestamp = json["entry_timestamp"].to_f
          exit_timestamp = json["exit_timestamp"].to_f
          metric_name =  json["metric_name"]       
          params = json["params"]
          
          called_segments = json["called_segments"]
        end
        segment = Segment.new(entry_timestamp, metric_name, id_generator.next_id)
        segment.end_trace exit_timestamp
        if params
          segment.send :params=, HashWithIndifferentAccess.new(params)
        end
        if called_segments
          called_segments.each do |child|
            segment.add_called_segment(self.from_json(child, id_generator))
          end
        end
        segment
      end
      
      def path_string
        "#{metric_name}[#{called_segments.collect {|segment| segment.path_string }.join('')}]"
      end
      def to_s_compact
        str = ""
        str << metric_name
        if called_segments.any?
          str << "{#{called_segments.map { | cs | cs.to_s_compact }.join(",")}}"
        end
        str
      end
      def to_debug_str(depth)
        tab = "  " * depth 
        s = tab.clone
        s << ">> #{'%3i ms' % (@entry_timestamp*1000)} [#{self.class.name.split("::").last}] #{metric_name} \n"
        unless params.empty?
          params.each do |k,v|
            s << "#{tab}    -#{'%-16s' % k}: #{v.to_s[0..80]}\n"
          end
        end
        called_segments.each do |cs|
          s << cs.to_debug_str(depth + 1)
        end
        s << tab + "<< "
        s << case @exit_timestamp
          when nil then ' n/a'
          when Numeric then '%3i ms' % (@exit_timestamp*1000)
          else @exit_timestamp.to_s
        end
        s << " #{metric_name}\n"
      end
      
      def called_segments
        @called_segments || EMPTY_ARRAY
      end
      
      # return the total duration of this segment
      def duration
        (@exit_timestamp - @entry_timestamp).to_f
      end
      
      # return the duration of this segment without 
      # including the time in the called segments
      def exclusive_duration
        d = duration
        
        if @called_segments
          @called_segments.each do |segment|
            d -= segment.duration
          end
        end
        d
      end
      def count_segments
        count = 1
        @called_segments.each { | seg | count  += seg.count_segments } if @called_segments
        count
      end
      # Walk through the tree and truncate the segments
      def truncate(max)
        return max unless @called_segments
        i = 0
        @called_segments.each do | segment |
          max = segment.truncate(max)
          max -= 1
          if max <= 0
            @called_segments = @called_segments[0..i]
            break
          else
            i += 1
          end
        end
        max
      end

      def []=(key, value)
        # only create a parameters field if a parameter is set; this will save
        # bandwidth etc as most segments have no parameters
        params[key] = value
      end
        
      def [](key)
        params[key]
      end
      
      def params
        @params ||= {}
      end
      
      # call the provided block for this segment and each 
      # of the called segments
      def each_segment(&block)
        block.call self
        
        if @called_segments
          @called_segments.each do |segment|
            segment.each_segment(&block)
          end
        end
      end
      
      def find_segment(id)
        return self if @segment_id == id
        called_segments.each do |segment|
          found = segment.find_segment(id)
          return found if found
        end
        nil
      end
      
      # perform this in the runtime environment of a managed application, to explain the sql
      # statement(s) executed within a segment of a transaction sample.
      # returns an array of explanations (which is an array rows consisting of 
      # an array of strings for each column returned by the the explain query)
      # Note this happens only for statements whose execution time exceeds a threshold (e.g. 500ms)
      # and only within the slowest transaction in a report period, selected for shipment to RPM
      def explain_sql        
        sql = params[:sql]
        return nil unless sql && params[:connection_config]
        statements = sql.split(";\n")
        explanations = []
        statements.each do |statement|
          if statement.split($;, 2)[0].upcase == 'SELECT'
            explain_resultset = []
            begin
              connection = NewRelic::TransactionSample.get_connection(params[:connection_config])    
              if connection
                # The resultset type varies for different drivers.  Only thing you can count on is
                # that it implements each.  Also: can't use select_rows because the native postgres
                # driver doesn't know that method.
                explain_resultset = connection.execute("EXPLAIN #{statement}") if connection
                rows = []
                # Note: we can't use map.
                # Note: have to convert from native column element types to string so we can
                # serialize.  Esp. for postgresql.
                # Can't use map.  Suck it up.
                if explain_resultset.respond_to?(:each)
                  explain_resultset.each { | row | rows << row.map(&:to_s) }
                else
                  rows << [ explain_resultset ]
                end
                explanations << rows
                # sleep for a very short period of time in order to yield to the main thread
                # this is because a remote database call will likely hang the VM
                sleep 0.05
              end
            rescue => e
              handle_exception_in_explain(e)
            end
          end
        end

        explanations
      end

      def handle_exception_in_explain(e)
        x = 1 # this is here so that code coverage knows we've entered this block
        # swallow failed attempts to run an explain.  One example of a failure is the
        # connection for the sql statement is to a different db than the default connection
        # specified in AR::Base
      end
      def obfuscated_sql
        TransactionSample.obfuscate_sql(params[:sql])
      end
      
      def called_segments=(segments)
        @called_segments = segments
      end
      
      protected
        def parent_segment=(s)
          @parent_segment = s
        end
        def params=(p)
          @params = p
        end
    end

    class FakeSegment < Segment
      public :parent_segment=
    end

    class SummarySegment < Segment
      
      
      def initialize(segment)
        super segment.entry_timestamp, segment.metric_name, nil
        
        add_segments segment.called_segments
        
        end_trace segment.exit_timestamp
      end
      
      def add_segments(segments)
        segments.collect do |segment|
          SummarySegment.new(segment)
        end.each {|segment| add_called_segment(segment)}
      end
      
    end
    
    class CompositeSegment < Segment
      attr_reader :detail_segments
      
      def initialize(segments)
        summary = SummarySegment.new(segments.first)
        super summary.entry_timestamp, "Repeating pattern (#{segments.length} repeats)", nil
        
        summary.end_trace(segments.last.exit_timestamp)
        
        @detail_segments = segments.clone
        
        add_called_segment(summary)
        end_trace summary.exit_timestamp
      end
      
      def detail_segments=(segments)
        @detail_segments = segments
      end
      
    end
    
    class << self
      def obfuscate_sql(sql)
        NewRelic::Agent.instance.obfuscator.call(sql) 
      end
      
      
      def get_connection(config)
        @@connections ||= {}
        
        connection = @@connections[config]
        
        return connection if connection
        
        begin
          connection = ActiveRecord::Base.send("#{config[:adapter]}_connection", config)
          @@connections[config] = connection
        rescue => e
          NewRelic::Agent.agent.log.error("Caught exception #{e} trying to get connection to DB for explain. Control: #{config}")
          NewRelic::Agent.agent.log.error(e.backtrace.join("\n"))
          nil
        end
      end
      
      def close_connections
        @@connections ||= {}
        @@connections.values.each do |connection|
          begin
            connection.disconnect!
          rescue
          end
        end
        
        @@connections = {}
      end
      
    end

    attr_accessor :profile
    attr_reader :root_segment
    attr_reader :params
    attr_reader :sample_id
    
    def initialize(time = Time.now.to_f, sample_id = nil)
      @sample_id = sample_id || object_id
      @start_time = time
      @root_segment = create_segment 0.0, "ROOT"
      @params = {}
      @params[:request_params] = {}
    end

    def count_segments
      @root_segment.count_segments - 1    # don't count the root segment
    end
    
    def truncate(max)
      original_count = count_segments
      
      return if original_count <= max
      
      @root_segment.truncate(max-1)
      
      if params[:segment_count].nil?
        params[:segment_count] = original_count
      end
    end
    
    # offset from start of app
    def timestamp
      @start_time - @@start_time.to_f
    end
    
    # Used in the server only
    def to_json(options = {}) #:nodoc:
      map = {:sample_id => @sample_id,
        :start_time => @start_time,
        :root_segment => @root_segment}
      if @params && !@params.empty?
        map[:params] = @params  
      end
      map.to_json
    end
    
    # Used in the Server only
    def self.from_json(json) #:nodoc:
      json = ActiveSupport::JSON.decode(json) if json.is_a?(String)
      
      if json.is_a?(Array)
        start_time = json[0].to_f / 1000
        custom_params = HashWithIndifferentAccess.new(json[2])
        params = {:request_params => HashWithIndifferentAccess.new(json[1]), 
              :custom_params => custom_params}
        cpu_time = custom_params.delete(:cpu_time)
        sample_id = nil
        params[:cpu_time] = cpu_time.to_f / 1000 if cpu_time
        root = json[3]
      else
        start_time = json["start_time"].to_f 
        sample_id = json["sample_id"].to_i
        params = json["params"] 
        root = json["root_segment"]
      end
      
      sample = TransactionSample.new(start_time, sample_id)
      
      if params
        sample.send :params=, HashWithIndifferentAccess.new(params)
      end
      if root
        sample.send :root_segment=, Segment.from_json(root, IDGenerator.new)
      end
      sample
    end

    def start_time
      Time.at(@start_time)
    end
    
    def path_string
      @root_segment.path_string
    end
    
    def create_segment(relative_timestamp, metric_name, segment_id = nil)
      raise TypeError.new("Frozen Transaction Sample") if frozen?
      NewRelic::TransactionSample::Segment.new(relative_timestamp, metric_name, segment_id)    
    end
    
    def duration
      root_segment.duration
    end
    
    def each_segment(&block)
      @root_segment.each_segment(&block)
    end
    
    def to_s_compact
      @root_segment.to_s_compact
    end
    
    def find_segment(id)
      @root_segment.find_segment(id)
    end
    
    def to_s
      s = "Transaction Sample collected at #{start_time}\n"
      s << "  {\n"
      s << "  Path: #{params[:path]} \n"
      
      params.each do |k,v|
        next if k == :path
        s << "  #{k}: " <<
        case v
          when Enumerable then v.map(&:to_s).sort.join("; ")
          when String then v
          when Float then '%6.3s' % v
          when nil then ''
        else
          raise "unexpected value type for #{k}: '#{v}' (#{v.class})"
        end << "\n"
      end
      s << "  }\n\n"
      s <<  @root_segment.to_debug_str(0)
    end
    
    # return a new transaction sample that treats segments
    # with the given regular expression in their name as if they
    # were never called at all.  This allows us to strip out segments
    # from traces captured in development environment that would not
    # normally show up in production (like Rails/Application Code Loading)
    def omit_segments_with(regex)
      regex = Regexp.new(regex)
      
      sample = TransactionSample.new(@start_time, sample_id)
      
      params.each {|k,v| sample.params[k] = v}
        
      delta = build_segment_with_omissions(sample, 0.0, @root_segment, sample.root_segment, regex)
      sample.root_segment.end_trace(@root_segment.exit_timestamp - delta)
      sample.profile = self.profile
      sample
    end
    
    # return a new transaction sample that can be sent to the RPM service.
    # this involves potentially one or more of the following options 
    #   :explain_sql : run EXPLAIN on all queries whose response times equal the value for this key
    #       (for example :explain_sql => 2.0 would explain everything over 2 seconds.  0.0 would explain everything.)
    #   :keep_backtraces : keep backtraces, significantly increasing size of trace (off by default)
    #   :obfuscate_sql : clear sql fields of potentially sensitive values (higher overhead, better security)
    def prepare_to_send(options={})
      sample = TransactionSample.new(@start_time, sample_id)
      
      sample.params.merge! self.params
      
      begin
        build_segment_for_transfer(sample, @root_segment, sample.root_segment, options)
      ensure
        self.class.close_connections
      end
      
      sample.root_segment.end_trace(@root_segment.exit_timestamp) 
      sample
    end
    
    def analyze
      sample = self
      original_path_string = nil
      loop do
        original_path_string = sample.path_string.to_s
        new_sample = sample.dup
        new_sample.root_segment = sample.root_segment.dup
        new_sample.root_segment.called_segments = analyze_called_segments(root_segment.called_segments)
        sample = new_sample
        return sample if sample.path_string.to_s == original_path_string
      end
      
    end
    
  protected
    def root_segment=(segment)
      @root_segment = segment
    end
    def params=(params)
      @params = params
    end

  private
  
    def analyze_called_segments(called_segments)
      path = nil
      like_segments = []
      
      segments = [] 
      
      called_segments.each do |segment|
        segment = segment.dup
        segment.called_segments = analyze_called_segments(segment.called_segments)
        
        current_path = segment.path_string
        if path == current_path 
          like_segments << segment
        else
          segments += summarize_segments(like_segments)

          like_segments.clear
          like_segments << segment
          path = current_path
        end
      end
      segments += summarize_segments(like_segments)
      
      segments
    end
    
    def summarize_segments(like_segments)
      if like_segments.length > COLLAPSE_SEGMENTS_THRESHOLD
        [CompositeSegment.new(like_segments)]
      else
        like_segments
      end
    end
    
    def build_segment_with_omissions(new_sample, time_delta, source_segment, target_segment, regex)
      source_segment.called_segments.each do |source_called_segment|
        # if this segment's metric name matches the given regular expression, bail
        # here and increase the amount of time that we reduce the target sample with
        # by this omitted segment's duration.
        do_omit = regex =~ source_called_segment.metric_name
        
        if do_omit
          time_delta += source_called_segment.duration
        else
          target_called_segment = new_sample.create_segment(
                source_called_segment.entry_timestamp - time_delta, 
                source_called_segment.metric_name,
                source_called_segment.segment_id)
          
          target_segment.add_called_segment target_called_segment
          source_called_segment.params.each do |k,v|
            target_called_segment[k]=v
          end
          
          time_delta = build_segment_with_omissions(
                new_sample, time_delta, source_called_segment, target_called_segment, regex)
          target_called_segment.end_trace(source_called_segment.exit_timestamp - time_delta)
        end
      end
      
      return time_delta
    end

    # see prepare_to_send for what we do with options
    def build_segment_for_transfer(new_sample, source_segment, target_segment, options)
      source_segment.called_segments.each do |source_called_segment|
        target_called_segment = new_sample.create_segment(
              source_called_segment.entry_timestamp,
              source_called_segment.metric_name,
              source_called_segment.segment_id)

        target_segment.add_called_segment target_called_segment
        source_called_segment.params.each do |k,v|
        case k
          when :backtrace
            target_called_segment[k]=v if options[:keep_backtraces]
          when :sql
            sql = v

            # run an EXPLAIN on this sql if specified.
            if options[:explain_enabled] && options[:explain_sql] && source_called_segment.duration > options[:explain_sql].to_f
              target_called_segment[:explanation] = source_called_segment.explain_sql
            end
            
            target_called_segment[:sql]=sql if options[:record_sql] == :raw
            target_called_segment[:sql_obfuscated] = TransactionSample.obfuscate_sql(sql) if options[:record_sql] == :obfuscated
          when :connection_config
            # don't copy it
          else
            target_called_segment[k]=v 
          end
        end

        build_segment_for_transfer(new_sample, source_called_segment, target_called_segment, options)
        target_called_segment.end_trace(source_called_segment.exit_timestamp)
      end
    end
    
    # Generates segment ids for json transaction segments
    class IDGenerator
      def initialize
        @next_id = 0        
      end
      def next_id
        @next_id += 1
      end
    end
  end
end
