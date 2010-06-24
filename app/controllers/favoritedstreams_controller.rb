class FavoritedstreamsController < ApplicationController
  def create
    # Check if the stream to favorite exists.
    begin
      Stream.find params[:id]
    rescue
      flash[:error] = "<strong>Could not favorite stream</strong><span>Stream does not exist.</span>"
      redirect_to :controller => "streams"
      return
    end

    favorite = FavoritedStream.new
    favorite.stream_id = params[:id]
    favorite.user_id = current_user.id

    if favorite.save
      flash[:notice] = "Stream has been added to favorites."
    else
      flash[:error] = "Could not add stream to favorites."
    end

    redirect_to :controller => "streams", :action => "show", :id => params[:id]
  end

  def destroy
    favorite = FavoritedStream.find_by_stream_id_and_user_id params[:id], current_user.id
    if favorite.destroy
      flash[:notice] = "Favorite has been removed."
    else
      flash[:error] = "Could not remove favorite."
    end
    redirect_to :controller => "streams", :action => "show", :id => params[:id]
  end
end
