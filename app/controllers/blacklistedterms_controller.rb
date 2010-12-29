class BlacklistedtermsController < ApplicationController
  filter_access_to :create
  filter_access_to :destroy

  def create
    term = BlacklistedTerm.new params[:blacklisted_term]
    if term.save
      flash[:notice] = "Term has been added to blacklist."
    else
      flash[:error] = "Could not add term to blacklist!"
    end
    #redirect_to :controller => "blacklists", :action => "show", :id => params[:blacklisted_term][:blacklist_id]
    redirect_to blacklist_path(params[:blacklist_id])
  end
  
  def destroy
    term = BlacklistedTerm.find params[:id]
    if term.destroy
      flash[:notice] = "Term has been removed from blacklist."
    else
      flash[:error] = "Could not remove term from blacklist."
    end
    #redirect_to :controller => "blacklists", :action => "show", :id => params[:blacklist_id]
    redirect_to blacklist_path(params[:blacklist_id])
  end
end
