class StreamsController < ApplicationController
  def index
    @new_stream = Stream.new
    @streams = Stream.all
  end

  def show
    @stream = Stream.find params[:id]
    @messages = Message.all_of_stream @stream.id, params[:page]
    @new_rule = Streamrule.new
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

  def destroy
    begin
      Streamrule.delete_all [ "stream_id = ?", params[:id] ]
      stream = Stream.find params[:id]
      stream.destroy
      flash[:notice] = "Stream has been deleted"
    rescue
      flash[:error] = "Could not delete stream"
    end
    redirect_to :action => "index"
  end

end
