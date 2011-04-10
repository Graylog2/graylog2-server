class FacilitiesController < ApplicationController
  def index
    @facilities = Facility.get_all
    @new_facility = Facility.new
  end

  def create
    facility = Facility.new(params[:facility])
    if facility.save
      flash[:notice] = "Facility has been added."
    else
      flash[:error] = "Could not add facility!"
    end

    redirect_to :action => "index"
  end

  def destroy
    # Deleting standard facilities is not allowed.
    if Facility.standards.include?(params[:id].to_i)
      flash[:error] = "Deleting standard facilities is not allowed."
      redirect_to :action => "index"
      return
    end

    facility = Facility.find_by_number(params[:id])
    if facility.destroy
      flash[:notice] = "Facility has been deleted."
    else
      flash[:error] = "Could not delete facility!"
    end

    redirect_to :action => "index"
  end

  def changetitle
    facility = Facility.find_by_number(params[:id])

    # Add if facility is a standard facility and does not yet exist in DB.
    if facility.blank?
      facility = Facility.new
      facility.title = params[:title]
      facility.number = params[:id]
    end

    facility.title = params[:title]

    if facility.save
      flash[:notice] = "Title has been changed."
    else
      flash[:error] = "Could not change title!"
    end

    redirect_to :action => "index"
  end
end
