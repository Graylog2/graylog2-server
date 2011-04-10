class ForwardersController < ApplicationController
  filter_access_to :all

  before_filter :fetch_stream

  def create
    fwd = @stream.forwarders.new(params[:forwarder])
    if fwd.save
      flash[:notice] = "Forwarder has been added."
    else
      flash[:error] = "Could not add forwarder."
    end

    redirect_to forward_stream_path(@stream)
  end

  def destroy
    fwd = @stream.forwarders.find(:first, :conditions => {:_id => BSON::ObjectId(params[:id])})
    if fwd.destroy
      flash[:notice] = "Forwarder has been removed from stream."
    else
      flash[:error] = "Could not remove forwarder from stream."
    end
    redirect_to forward_stream_path(@stream)
  end

  protected
  def fetch_stream
    if params[:stream_id]
      @stream = Stream.find_by_id(params[:stream_id])
    end
  end
end
