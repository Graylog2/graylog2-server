class StreamcategoriesController < ApplicationController

  def create
    category = Streamcategory.new(params[:streamcategory])

    if category.save
      flash[:notice] = "Stream category has been created."
    else
      flash[:error] = "Could not create stream category!"
    end

    redirect_to :controller => "streams", :action => "index"
  end

end
