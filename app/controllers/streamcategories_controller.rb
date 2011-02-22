class StreamcategoriesController < ApplicationController

  def index
    @new_streamcategory = Streamcategory.new
    @categories = Streamcategory.all
  end

  def create
    category = Streamcategory.new(params[:streamcategory])

    if category.save
      flash[:notice] = "Stream category has been created."
    else
      flash[:error] = "Could not create stream category!"
    end

    redirect_to :action => "index"
  end

  def destroy
    category = Streamcategory.find(params[:id])

    if category.destroy
      flash[:notice] = "Stream category has been deleted."
    else
      flash[:error] = "Could not delete stream category!"
    end
    
    redirect_to :action => "index"
  end

end
