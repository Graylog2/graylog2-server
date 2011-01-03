class AlertedstreamsController < ApplicationController

  def toggle
    stream_id = params[:id]
    # Check if the stream to favorite exists.
    stream = nil 
    begin
      stream = Stream.find(stream_id)
    rescue
      flash[:error] = "<strong>Could not favorite stream</strong><span>Stream does not exist.</span>"
      redirect_to streams_path
      return
    end

    if stream.alerted?(current_user)
      destroy(stream_id)
    else
      create(stream_id)
    end
    
    # Only intended to be called via AJAX.
    render :text => ""
  end

  private

  def create(stream_id)
    alert = AlertedStream.new
    alert.stream_id = stream_id
    alert.user_id = current_user.id

    alert.save
  end

  def destroy(stream_id)
    alert = AlertedStream.find_by_stream_id_and_user_id(stream_id, current_user.id)
    alert.destroy
  end
end
