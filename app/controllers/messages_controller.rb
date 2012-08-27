class MessagesController < ApplicationController
  before_filter :do_scoping

  ignore_session_on_json :index, :show

  filter_access_to :all

  # XXX ELASTIC clean up triple-duplicated quickfilter shit
  def do_scoping
    (!params[:showall].blank? and params[:showall] == "true") ? showall = true : showall = false
    
    if params[:host_id]
      @scoping = :host
      block_access_for_non_admins

      @host = Host.find(:first, :conditions => {:host=> params[:host_id]})
      
      @total_count = MessageGateway.host_count(@host.host)

      if params[:filters].blank?
        @messages = MessageGateway.all_of_host_paginated(@host.host, params[:page], :all => showall)
      else
        @additional_filters = Quickfilter.extract_additional_fields_from_request(params[:filters])
        @messages = MessageGateway.all_by_quickfilter(params[:filters], params[:page], :hostname => @host.host)
        @quickfilter_result_count = @messages.total_result_count
      end
    elsif params[:stream_id]
      @scoping = :stream
      @stream = Stream.find_by_id_or_name(params[:stream_id])
      @is_favorited = current_user.favorite_streams.include?(params[:stream_id])

      # Check streams for reader.
      block_access_for_non_admins if !@stream.accessable_for_user?(current_user)
        
      @total_count = MessageGateway.stream_count(@stream.id)
      
      if params[:filters].blank?
        @messages = MessageGateway.all_of_stream_paginated(@stream.id, params[:page], :all => showall)
      else
        @additional_filters = Quickfilter.extract_additional_fields_from_request(params[:filters])
        @messages = MessageGateway.all_by_quickfilter(params[:filters], params[:page], :stream_id => @stream.id)
        @quickfilter_result_count = @messages.total_result_count
      end
    else
      @scoping = :messages
      unless (params[:action] == "show")
        block_access_for_non_admins
      end
        
      @total_count = MessageGateway.total_count # XXX ELASTIC Possibly read cached from first all_paginated result?!

      if params[:filters].blank?
        @messages = MessageGateway.all_paginated(params[:page], :all => showall)
      else
        @additional_filters = Quickfilter.extract_additional_fields_from_request(params[:filters])
        @messages = MessageGateway.all_by_quickfilter(params[:filters], params[:page])
        @quickfilter_result_count = @messages.total_result_count
      end
    end
  rescue Tire::Search::SearchRequestFailed
      flash[:error] = "Syntax error in search query or empty index."
      @messages = MessageResult.new
      @total_count = 0
      @quickfilter_result_count = @messages.total_result_count
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
    @load_messages = true

    if ::Configuration.allow_version_check
      @last_version_check = current_user.last_version_check
    end
    respond_to do |format|
        format.html
        format.json { render :json => @messages }
    end
  end

  def show
    @has_sidebar = true
    @load_flot = true
    @load_messages = true

    @message = MessageGateway.retrieve_by_id(params[:id])
    @terms = MessageGateway.analyze(@message.message)

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
    render :status => :forbidden, :text => "forbidden" and return if !::Configuration.allow_deleting

    if MessageGateway.delete_message(params[:id])
      flash[:notice] = "Message has been deleted."
    else
      flash[:error] = "Could not delete message."
    end

    redirect_to :action => "index"
  end

  def showrange
    @has_sidebar = true
    @load_flot = true
    @use_backtotop = true

    @from = Time.at(params[:from].to_i-Time.now.utc_offset)
    @to = Time.at(params[:to].to_i-Time.now.utc_offset)

    @messages = MessageGateway.all_in_range(params[:page], @from.to_i, @to.to_i)
    @total_count = @messages.total_result_count
  end

  def realtime
    render :text => "enable realtime in general.yml", :status => :forbidden and return if !::Configuration.realtime_enabled?
    @content_class = "console"
    @has_realtime = true
    @websocket_target = ::Configuration.realtime_websocket_url + "/overall" rescue nil
    @auth_token = ::Configuration.realtime_websocket_token
  end

end
