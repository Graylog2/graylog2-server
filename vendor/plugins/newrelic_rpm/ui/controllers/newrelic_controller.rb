class NewrelicController < ActionController::Base
  include NewrelicHelper
  helper NewrelicHelper
  
  # See http://wiki.rubyonrails.org/rails/pages/Safe+ERB:
  # We don't need to worry about checking taintedness
  def initialize(*args)
    @skip_checking_tainted = true
    super *args
  end
  
  # do not include any filters inside the application since there might be a conflict
  if respond_to? :filter_chain
    filters = filter_chain.collect do |f|
      if f.respond_to? :filter
        # rails 2.0
        f.filter
      elsif f.respond_to? :method
        # rails 2.1
        f.method
      else
        fail "Unknown filter class. Please send this exception to support@newrelic.com"
      end
    end
    skip_filter filters
  end
  
  # for this controller, the views are located in a different directory from
  # the application's views.
  view_path = File.join(File.dirname(__FILE__), '..', 'views')
  if respond_to? :append_view_path # rails 2.1+
    self.append_view_path view_path
  elsif respond_to? :view_paths   # rails 2.0+
    self.view_paths << view_path
  else                                      # rails <2.0
    self.template_root = view_path
  end
  
  layout "newrelic_default"
  
  write_inheritable_attribute('do_not_trace', true)
  
  def profile
    NewRelic::Control.instance.profiling = params['start'] == 'true'
    get_samples
    redirect_to :action => 'index'
  end
  
  def file
    file_name=Array(params[:file]).join
    file_name=~/^.*[.]([^.]*)$/
    ext=$1
    case ext
      when 'css' then
        forward_to_file '/newrelic/stylesheets/', 'text/css'
      when 'gif','jpg','png' then
        forward_to_file '/newrelic/images/', "image/#{ext}"
      when 'js' then
        forward_to_file '/newrelic/javascript/', 'text/javascript'
      else
        raise "Unknown type '#{ext}' (#{file_name})"
    end
  end

  def index
    get_samples
  end
  
  def threads
    
  end
  
  def reset
    NewRelic::Agent.instance.transaction_sampler.reset!
    redirect_to :action => 'index'
  end

  def show_sample_detail
    show_sample_data
  end
  
  def show_sample_summary
    show_sample_data
  end
  
  def show_sample_sql
    show_sample_data
  end
  
  
  def explain_sql
    get_segment
    
    render :action => "sample_not_found" and return unless @sample 
    
    @sql = @segment[:sql]
    @trace = @segment[:backtrace]
    
    if NewRelic::Agent.agent.record_sql == :obfuscated  
      @obfuscated_sql = @segment.obfuscated_sql
    end
    
    explanations = @segment.explain_sql
    if explanations
      @explanation = explanations.first 
      if !@explanation.blank?
        first_row = @explanation.first
        # Show the standard headers if it looks like a mysql explain plan
        # Otherwise show blank headers
        if first_row.length < NewRelic::MYSQL_EXPLAIN_COLUMNS.length
          @row_headers = nil
        else
          @row_headers = NewRelic::MYSQL_EXPLAIN_COLUMNS
        end
      end
    end
  end
  
  # show the selected source file with the highlighted selected line
  def show_source
    @filename = params[:file]
    line_number = params[:line].to_i
    
    if !File.readable?(@filename)
      @source="<p>Unable to read #{@filename}.</p>"
      return
    end
    begin
      file = File.new(@filename, 'r')
    rescue => e
      @source="<p>Unable to access the source file #{@filename} (#{e.message}).</p>"
      return
    end
    @source = ""
    
    @source << "<pre>"
    file.each_line do |line|
      # place an anchor 6 lines above the selected line (if the line # < 6)
      if file.lineno == line_number - 6
        @source << "</pre><pre id = 'selected_line'>"
        @source << line.rstrip
        @source << "</pre><pre>"
        
        # highlight the selected line
      elsif file.lineno == line_number
        @source << "</pre><pre class = 'selected_source_line'>"
        @source << line.rstrip
        @source << "</pre><pre>"
      else
        @source << line
      end
    end
  end
  
  private 
  
  # root path is relative to plugin newrelic_rpm/ui/views directory.
  def forward_to_file(root_path, content_type='ignored anyway')
    file = File.expand_path(File.join(__FILE__,"../../views", root_path, params[:file]))
    last_modified = File.mtime(file)
    date_check = request.respond_to?(:headers) ? request.headers['if-modified-since'] : request.env['HTTP_IF_MODIFIED_SINCE']
    if date_check && Time.parse(date_check) >= last_modified
      expires_in 24.hours
      head :not_modified, 
      :last_modified => last_modified,
      :type => 'text/plain'
    else
      response.headers['Last-Modified'] = last_modified.to_formatted_s(:rfc822)
      expires_in 24.hours
      send_file file, :content_type => mime_type_from_extension(file), :disposition => 'inline' #, :filename => File.basename(file)
    end
  end
  
  def show_sample_data
    get_sample
    
    render :action => "sample_not_found" and return unless @sample 
    
    @request_params = @sample.params[:request_params] || {}
    @custom_params = @sample.params[:custom_params] || {}

    controller_metric = @sample.root_segment.called_segments.first.metric_name
    
    controller_segments = controller_metric.split('/')
    @sample_controller_name = controller_segments[1..-2].join('/').camelize+"Controller"
    @sample_action_name = controller_segments[-1].underscore
    
    render :action => :show_sample
  end
  
  def get_samples
    @samples = NewRelic::Agent.instance.transaction_sampler.samples.select do |sample|
      sample.params[:path] != nil
    end
    
    return @samples = @samples.sort{|x,y| y.omit_segments_with('(Rails/Application Code Loading)|(Database/.*/.+ Columns)').duration <=>
        x.omit_segments_with('(Rails/Application Code Loading)|(Database/.*/.+ Columns)').duration} if params[:h]
    return @samples = @samples.sort{|x,y| x.params[:uri] <=> y.params[:uri]} if params[:u]
    @samples = @samples.reverse
  end
  
  def get_sample
    get_samples
    sample_id = params[:id].to_i
    @samples.each do |s|
      if s.sample_id == sample_id
        @sample = stripped_sample(s)
        return 
      end
    end
  end
  
  def get_segment
    get_sample
    return unless @sample
    
    segment_id = params[:segment].to_i
    @segment = @sample.find_segment(segment_id)
  end
end
