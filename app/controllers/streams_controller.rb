class StreamsController < ApplicationController
  before_filter :tabs, :except => :index
  
  def index
    @new_stream = Stream.new
    @streams = Stream.all
  end

  def show
    @stream = Stream.find params[:id]
    @messages = Message.all_of_stream @stream.id, params[:page]
    @total_count = Message.count_stream @stream.id

    # Find out if this stream is favorited by the current user.
    FavoritedStream.find_by_stream_id_and_user_id(params[:id], current_user.id).blank? ? @is_favorited = false : @is_favorited = true
  end

  def rules
    @stream = Stream.find params[:id]
    @new_rule = Streamrule.new
  end

  def settings
    @stream = Stream.find params[:id]
  end

  def togglealarmactive
    stream = Stream.find(params[:id])
    if stream.alarm_active
      stream.alarm_active = false
    else
      stream.alarm_active = true
    end

    stream.save

    # Intended to be called via AJAX only.
    render :text => ""
  end
  
  def togglealarmforce
    stream = Stream.find(params[:id])
    if stream.alarm_force
      stream.alarm_force = false
    else
      stream.alarm_force = true
    end

    stream.save

    # Intended to be called via AJAX only.
    render :text => ""
  end

  def setalarmvalues
    stream = Stream.find(params[:id])

    unless params[:limit].blank? or params[:timespan].blank?
      stream.alarm_limit = params[:limit]
      stream.alarm_timespan = params[:timespan]

      if stream.save
        flash[:notice] = "Alarm settings updated."
      else
        flash[:error] = "Could not update alarm settings."
      end
    else
        flash[:error] = "Could not update alarm settings: Missing parameters."
    end

    redirect_to :action => "settings", :id => params[:id]
  end

  def create
    new_stream = Stream.new params[:stream]
    if new_stream.save
      flash[:notice] = "Stream has been created"
    else
      flash[:error] = "Could not create stream"
    end
    redirect_to :action => "index"
  end
  
  def rename
    stream = Stream.find params[:stream_id]
    stream.title = params[:title]
    
    if stream.save
      flash[:notice] = "Stream has been renamed."
    else
      flash[:error] = "Could not rename stream."
    end

    redirect_to :controller => "streams", :action => "settings", :id => params[:stream_id]
  end

  def destroy
    stream = Stream.find params[:id]
    if stream.destroy
      flash[:notice] = "Stream has been deleted"
    else
      flash[:error] = "Could not delete stream"
    end

    redirect_to :action => "index"
  end
  
#  def get_hosts_statistic
#    throw "Missing stream ID" if params[:id].blank?
#
#    total_message_count = Stream.get_message_count(params[:id]).to_i
#    hosts = Stream.get_distinct_hosts params[:id]
#
#    ready_hosts = Array.new
#    hosts.each do |host|
#      message_count = Stream.get_count_by_host(params[:id], host).to_i
#      # Thanks to Sarah and her wicked percentage calculation skills. (<3)
#      percent = 100-(((total_message_count-message_count)*100)/total_message_count)
#      ready_hosts << { 'name' => host, 'percent' => percent.to_i }
#    end
#
#    # Sort the result.
#    ready_hosts = ready_hosts.sort { |a,b| b['percent'] <=> a['percent'] }
#
#    if hosts.blank?
#      render :text => 'No messages found.'
#      return
#    end
#
#    render :partial => 'statistics', :locals => { :hosts => ready_hosts }
#  end

  def tabs
    @tabs = [ "Show", "Rules", "Analytics", "Settings" ]
  end
end
