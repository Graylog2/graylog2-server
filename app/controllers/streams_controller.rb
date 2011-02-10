class StreamsController < ApplicationController
  filter_resource_access
  before_filter :tabs, :except => :index
  
  def index
    @new_stream = Stream.new
    if current_user.role_symbols.include? :admin
      @streams = Stream.all
    else
      @streams = current_user.streams
    end
  end

  def show
    @has_sidebar = true
    @load_flot = 3

    if params[:filters].blank?
      @messages = Message.all_of_stream @stream.id, params[:page]
    else
      @additional_filters = Message.extract_additional_from_quickfilter(params[:filters])
      @messages = Message.by_stream(@stream).all_by_quickfilter params[:filters], params[:page]
    end
    
    @total_count = Message.count_stream @stream.id

    # Find out if this stream is favorited by the current user.
    @is_favorited = current_user.favorite_streams.include?(@stream)
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
    
    @messages = Message.all_of_stream_in_range(@stream.id, params[:page], @from.to_i, @to.to_i)
    @total_count = Message.count_all_of_stream_in_range(@stream.id, @from.to_i, @to.to_i)
  end

  def rules
    @stream = Stream.find params[:id]
    @new_rule = Streamrule.new
  end

  def analytics
    @load_flot = true
    @stream = Stream.find params[:id]
  end

  def settings
    @stream = Stream.find params[:id]
  end

  def setdescription
    @stream = Stream.find(params[:id])
    @stream.description = params[:description]

    if @stream.save
      flash[:notice] = "Description has been saved."
    else
      flash[:error] = "Could not save description."
    end
    redirect_to stream_path(params[:id])
  end

  def togglefavorited
    stream = Stream.find(params[:id])
    if !stream.favorited?(current_user)
      current_user.favorite_streams << stream
    else
      current_user.favorite_streams.delete(stream)
    end

    # Intended to be called via AJAX only.
    render :text => ""
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

    redirect_to settings_stream_path(stream)
  end

  def create
    new_stream = Stream.new params[:stream]
    if new_stream.save
      flash[:notice] = "Stream has been created"
    else
      flash[:error] = "Could not create stream"
    end
    redirect_to rules_stream_path(new_stream)
  end
  
  def rename
    stream = Stream.find params[:stream_id]
    stream.title = params[:title]
    
    if stream.save
      flash[:notice] = "Stream has been renamed."
    else
      flash[:error] = "Could not rename stream."
    end

    redirect_to settings_stream_path(stream)
  end

  def destroy
    stream = Stream.find params[:id]
    if stream.destroy
      flash[:notice] = "Stream has been deleted"
    else
      flash[:error] = "Could not delete stream"
    end
    
    redirect_to streams_path
  end
  
  def tabs
    @tabs = [ "Show", "Rules", "Analytics", "Settings" ]
  end
  
  def subscribe
    current_user.subscribed_streams << @stream
    render :js => {:status => :success}
  end
  
  def unsubscribe
    current_user.subscribed_streams.delete @stream
    render :js => {:status => :success}
  end
  
  def togglesubscription
    @stream.subscribed?(current_user) ? unsubscribe : subscribe
  end
end
