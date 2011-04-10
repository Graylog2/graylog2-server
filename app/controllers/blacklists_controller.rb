class BlacklistsController < ApplicationController
  filter_resource_access
  def index
    @blacklists = Blacklist.find :all

    @new_blacklist = Blacklist.new
  end

  def show
   @new_term = BlacklistedTerm.new
  end

  def create
    new_blacklist = Blacklist.new params[:blacklist]
    if new_blacklist.save
      flash[:notice] = "Blacklist has been saved"
    else
      flash[:error] = "Could not save blacklist"
    end
    redirect_to :action => "index"
  end

  def destroy
    begin
      blacklist = Blacklist.find(BSON::ObjectId(params[:id]))
      blacklist.destroy
      flash[:notice] = "Blacklist has been deleted"
    rescue
      flash[:error] = "Could not delete blacklist"
    end
    redirect_to :action => "index"
  end

end
