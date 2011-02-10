class MessagesController < ApplicationController
  before_filter :set_scoping
  filter_resource_access
  
  rescue_from MongoMapper::DocumentNotFound, :with => :not_found
  rescue_from BSON::InvalidObjectId, :with => :not_found
  
  def set_scoping
    if params[:host_id]
      @scope = Message.where(:host => Base64.decode64(params[:host_id]))
    elsif params[:stream_id]
      @scope = Message.by_stream(params[:stream_id])
    else
      @scope = Message
    end
  end
  
  def index
    @has_sidebar = true
    @load_flot = true
  
    if params[:filters].blank?
      @messages = @scope.all_with_blacklist params[:page]
    else
      @additional_filters = Message.extract_additional_from_quickfilter(params[:filters])
      @messages = @scope.all_by_quickfilter params[:filters], params[:page]
    end
    @total_count = Message.count_since(0)
    @total_blacklisted_terms = BlacklistedTerm.count
  end

  def show
    @has_sidebar = true
    @load_flot = true
    @message = Message.find params[:id]

    @comments = Messagecomment.all_matched(@message)

    if params[:partial]
      render :partial => "full_message"
      return
    end
  end

  def showrange
    @has_sidebar = true
    @load_flot = true
    
    begin
      @from = Time.at(params[:from].to_i-Time.now.utc_offset)
      @to = Time.at(params[:to].to_i-Time.now.utc_offset)
    rescue
      flash[:error] = "Missing or invalid range parameters."
    end
    @messages = Message.all_in_range(params[:page], @from.to_i, @to.to_i)
    @total_count = Message.count_all_in_range(@from.to_i, @to.to_i)
  end

  def around
    @message = Message.find!(params[:id])
    @has_sidebar = true
    @load_flot = true
    @nb = (params[:nb] || 100).to_i
    @messages = @message.around(@nb)
    
    respond_to do |format|
      format.html 
      format.text { 
        send_data @messages.map(&:message).join("\n"), :type => "text/plain", :filename => "#{@id.to_s}-#{@nb}.log"  
      }
    end
  end

  def getcompletemessage
    message = Message.find params[:id]
    render :text => CGI.escapeHTML(message.message)
  end
  
  def deletebystream
    begin
      conditions = Message.by_stream(params[:stream_id].to_i).criteria.to_hash
      throw "Missing conditions" if conditions.blank?

      Message.recalculate_host_counts
      Message.set(conditions, :deleted => true )

      flash[:notice] = "Messages have been deleted."
    rescue => e
      flash[:error] = "Could not delete messages."
    end
    
    redirect_to stream_path(params[:stream_id])
  end

  def deletebyquickfilter
    begin
      filters = JSON.parse(params[:filters])

      filters_with_symbols = Hash.new
      filters_with_symbols[:message] = filters["message"]
      filters_with_symbols[:facility] = filters["facility"]
      filters_with_symbols[:severity] = filters["severity"]
      filters_with_symbols[:host] = filters["host"]

      conditions = Message.all_by_quickfilter(filters_with_symbols, 0, 0).criteria.to_hash
      throw "Missing conditions" if conditions.blank?

      Message.recalculate_host_counts
      Message.set(conditions, :deleted => true )

      flash[:notice] = "Messages have been deleted."
    rescue
      flash[:error] = "Could not delete messages."
    end

    redirect_to :action => 'index'
  end

  # Get the count of new messages since a given UNIX timestamp
  def getnewmessagecount
    since = params[:since].to_i
    if since <= 0
      render :js => { 'status' => 'error', 'payload' => "Missing or invalid parameter: since" }.to_json
      return
    end
    render :js => { 'status' => 'success', 'payload' => Message.count_since(since) }.to_json
  end

end
