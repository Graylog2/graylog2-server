class MessagesController < ApplicationController
  before_filter :set_scoping

  filter_access_to :all

  rescue_from Mongoid::Errors::DocumentNotFound, :with => :not_found
  rescue_from BSON::InvalidObjectId, :with => :not_found

  protected
  def set_scoping
    if params[:host_id]
      block_access_for_non_admins

      @scope = Message.where(:host => params[:host_id])
      @host = Host.find(:first, :conditions => {:host=> params[:host_id]})
      @scoping = :host
    elsif params[:stream_id]
      @stream = Stream.find_by_id(params[:stream_id])

      # Check streams for reader.
      block_access_for_non_admins if !@stream.accessable_for_user?(current_user)

      @scope = Message.by_stream(@stream.id)
      @scoping = :stream
    elsif params[:hostgroup_id]
      block_access_for_non_admins

      @hostgroup = Hostgroup.find_by_id params[:hostgroup_id]
      @scope = Message.all_of_hostgroup @hostgroup, params[:page]
      @scoping = :hostgroup
    else
      unless (params[:action] == "show")
        block_access_for_non_admins
      end

      @scope = Message
      @scoping = :messages
    end
  end

  # Not possible to do this via before_filter because of scope decision by params hash
  def block_access_for_non_admins
    if current_user.role != "admin"
      flash[:error] = "You have no access rights for this section."
      redirect_to :controller => "streams", :action => "index"
    end
  end

  public
  def index
    @has_sidebar = true
    @load_flot = true
    @use_backtotop = true

    if Configuration.allow_version_check
      @last_version_check = current_user.last_version_check
    end

    if params[:filters].blank?
      @messages = @scope.all_paginated(params[:page])
    else
      @additional_filters = Message.extract_additional_from_quickfilter(params[:filters])
      @messages = @scope.all_by_quickfilter params[:filters], params[:page]
    end
    @total_count = @scope.count
    @last_message = @scope.order_by(:created_at.desc).limit(1).first #last :order => "created_at DESC"

    if params[:stream_id]
      @is_favorited = current_user.favorite_streams.include?(params[:stream_id])
    end
  end

  def show
    @has_sidebar = true
    @load_flot = true

    @message = @scope.where(:_id => BSON::ObjectId(params[:id])).all.first

    unless @message.accessable_for_user?(current_user)
      block_access_for_non_admins
    end

    @comments = Messagecomment.all_matched(@message)

    if params[:partial]
      render :partial => "full_message"
      return
    end
  end

  def destroy
    Message.where(:_id => BSON::ObjectId(params[:id])).update(:deleted => true)
    redirect_to :action => "index"
  end

  def showrange
    @has_sidebar = true
    @load_flot = true
    @use_backtotop = true

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
    @message = @scope.find_by_id(params[:id])
    @has_sidebar = true
    @load_flot = true
    @use_backtotop = true
    @nb = (params[:nb] || 100).to_i
    @messages = @message.around(@nb)

    respond_to do |format|
      format.html
      format.text {
        send_data render_to_string("messages/message_log.txt", :locals => {:messages => @messages}), :type => "text/plain", :filename => "#{@message.id.to_s}-#{@nb}.log"
        #send_data @messages.collect {|m| "#{m.created_at.to_s} #{m.host} #{m.facility} #{m.full_message}"}.join("\n"), :type => "text/plain", :filename => "#{@message.id.to_s}-#{@nb}.log"
      }
    end
  end

  def getcompletemessage
    message = Message.find params[:id]
    render :text => CGI.escapeHTML(message.message)
  end

  def deletebystream
    Message.where(:streams => BSON::ObjectId(params[:stream_id])).update_all(
      :deleted => true
    )

    Message.recalculate_host_counts

    redirect_to stream_path(params[:stream_id])
  end

  def deletebyquickfilter
    filters = JSON.parse(params[:filters])

    filters_with_symbols = Hash.new
    filters_with_symbols[:message] = filters["message"]
    filters_with_symbols[:facility] = filters["facility"]
    filters_with_symbols[:severity] = filters["severity"]
    filters_with_symbols[:host] = filters["host"]

    Message.all_by_quickfilter(filters_with_symbols, 0, 0, true).update_all(
      :deleted => true
    )

    Message.recalculate_host_counts

    flash[:notice] = "Messages have been deleted."

    redirect_to :action => 'index'
  end

  # Get the count of new messages since a given UNIX timestamp
  def getnewmessagecount
    since = params[:since].to_i
    if since <= 0
      render :js => { 'status' => 'error', 'payload' => "Missing or invalid parameter: since" }.to_json
      return
    end

    if params[:stream_id]
      stream = Stream.find_by_id params[:stream_id]
      if stream.nil?
        render :js => {'status' => 'error',
                       'payload' => "Cannot find stream with id #{params[:stream_id]}" }.to_json
        return
      end
      render :js => { 'status' => 'success', 'payload' => stream.message_count_since(since) }.to_json
    else
      render :js => { 'status' => 'success', 'payload' => Message.count_since(since) }.to_json
    end
  end

end
