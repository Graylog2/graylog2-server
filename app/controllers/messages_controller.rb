class MessagesController < ApplicationController
  before_filter :set_scoping
  filter_resource_access
  
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
      @messages = @scope.all_by_quickfilter params[:filters], params[:page]
    end
    @total_count =  Message.count_since(0)
    @total_blacklisted_terms = BlacklistedTerm.count
  end

  def show
    @has_sidebar = true
    @load_flot = true
    @message = Message.find params[:id]

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

  def getcompletemessage
    message = Message.find params[:id]
    render :text => CGI.escapeHTML(message.message)
  end
  
  def deletebystream
    begin
      conditions = Message.by_stream(params[:id].to_i).criteria.to_hash
      throw "Missing conditions" if conditions.blank?

      Message.set(conditions, :deleted => true )

      flash[:notice] = "Messages have been deleted."
    rescue => e
      flash[:error] = "Could not delete messages."
    end
    
    redirect_to :controller => "streams", :action => "show", :id => params[:id]
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

      Message.set(conditions, :deleted => true )

      flash[:notice] = "Messages have been deleted."
    rescue
      flash[:error] = "Could not delete messages."
    end

    redirect_to :action => 'index'
  end

end
