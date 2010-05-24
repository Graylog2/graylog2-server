class StreamsController < ApplicationController
  def index
    @new_stream = Stream.new

    @messages = Message.all_with_blacklist(10)
  end

  def create
    new_stream = Stream.new params[:stream]
    if new_stream.save
      flash[:notice] = "Stream has been saved"
    else
      flash[:error] = "Could not save stream"
    end
    redirect_to :action => "index"
  end
end
