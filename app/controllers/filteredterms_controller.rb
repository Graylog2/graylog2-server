class FilteredtermsController < ApplicationController

  def index
    @filteredterms = FilteredTerm.all
    @new_filteredterm = FilteredTerm.new
    @has_settings_tabs = true
  end

  def create
    term = FilteredTerm.new(params[:filtered_term])

    if term.save
      flash[:notice] = "Term has been added."
    else
      flash[:error] = "Could not add term!"
    end

    redirect_to :action => :index
  end

  def destroy
    term = FilteredTerm.find(params[:id])
    if term.delete
      flash[:notice] = "Term has been deleted."
    else
      flash[:error] = "Could not delete term!"
    end

    redirect_to :action => :index
  end

end
