class StreamcategoriesController < ApplicationController

  def index
    @new_streamcategory = Streamcategory.new
    @categories = Streamcategory.all
  end

  def rename
    @category = Streamcategory.find(params[:id])
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

  # Actually only renaming as it is the only attribute.
  def update
    category = Streamcategory.find(params[:id])
    category.title = params[:title] unless params[:title].blank?

    if category.save
      flash[:notice] = "Stream category has been renamed."
    else
      flash[:error] = "Could not rename stream category!"
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
