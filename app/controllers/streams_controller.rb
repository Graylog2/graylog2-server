class StreamsController < ApplicationController
  def index
    @new_stream = Stream.new

    @streams = Stream.all
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
end
