class AlertedstreamsController < ApplicationController

  # wtf is this shit? heavy refactoring needed here

  def toggle
    stream = nil
    begin
      stream = Stream.find_by_id(params[:id])
    rescue => e
      flash[:error] = "Could not favorite stream: Stream does not exist."
      redirect_to streams_path
      return
    end

    if stream.alerted?(current_user)
      destroy(stream.id)
    else
      create(stream.id)
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
    AlertedStream.where(:stream_id => stream_id, :user_id => current_user.id).destroy
  end
end
