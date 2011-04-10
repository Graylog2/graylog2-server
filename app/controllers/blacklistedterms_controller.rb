class BlacklistedtermsController < ApplicationController
  filter_access_to :all

  before_filter :load_blacklist

  def create
    term = @blacklist.blacklisted_terms.create params[:blacklisted_term]
    if term.save
      flash[:notice] = "Term has been added to blacklist."
    else
      flash[:error] = "Could not add term to blacklist!"
    end

    redirect_to blacklist_path(params[:blacklist_id])
  end

  def destroy
    term = @blacklist.blacklisted_terms.where({:_id => BSON::ObjectId(params[:id])})
    if term.destroy
      flash[:notice] = "Term has been removed from blacklist."
    else
      flash[:error] = "Could not remove term from blacklist."
    end

    redirect_to blacklist_path(params[:blacklist_id])
  end

  protected
  def load_blacklist
    @blacklist = Blacklist.find_by_id(params[:blacklist_id])
  end
end
