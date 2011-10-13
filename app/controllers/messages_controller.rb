# XXX ELASTIC "messages from today button" is not working

class MessagesController < ApplicationController
  before_filter :do_scoping

  filter_access_to :all

  rescue_from Mongoid::Errors::DocumentNotFound, :with => :not_found
  rescue_from BSON::InvalidObjectId, :with => :not_found

  def do_scoping
    if params[:host_id]
      @scoping = :host
      block_access_for_non_admins

      @scope = Message.where(:host => params[:host_id])
      @host = Host.find(:first, :conditions => {:host=> params[:host_id]})
    elsif params[:stream_id]
      @scoping = :stream
      @stream = Stream.find_by_id(params[:stream_id])
      @is_favorited = current_user.favorite_streams.include?(params[:stream_id])

      # Check streams for reader.
      block_access_for_non_admins if !@stream.accessable_for_user?(current_user)

      @messages = MessageGateway.all_of_stream_paginated(@stream.id, params[:page])
      @total_count = @messages.total
    elsif params[:hostgroup_id]
      @scoping = :hostgroup
      block_access_for_non_admins

      @hostgroup = Hostgroup.find_by_id params[:hostgroup_id]
      @scope = Message.all_of_hostgroup @hostgroup, params[:page]
    else
      @scoping = :messages
      unless (params[:action] == "show")
        block_access_for_non_admins
      end

      if params[:filters].blank?
        @messages = MessageGateway.all_paginated(params[:page])
      else
        @additional_filters = extract_additional_from_quickfilter(params[:filters])
        @messages = MessageGateway.all_by_quickfilter(params[:filters], params[:page])
      end
      @total_count = @messages.total
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
  end

  def show
    @has_sidebar = true
    @load_flot = true

    @message = MessageGateway.retrieve_by_id(params[:id])

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

    @from = Time.at(params[:from].to_i-Time.now.utc_offset)
    @to = Time.at(params[:to].to_i-Time.now.utc_offset)

    @messages = MessageGateway.all_in_range(params[:page], @from.to_i, @to.to_i)
    @total_count = @messages.total
    # XXX ELASTIC pagination is broken
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

end
