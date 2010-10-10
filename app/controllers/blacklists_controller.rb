class BlacklistsController < ApplicationController
  def index
    @blacklists = Blacklist.find :all

    @new_blacklist = Blacklist.new
  end

  def show
    @blacklist = Blacklist.find params[:id]
    @messages = Message.all_of_blacklist @blacklist.id, params[:page]
    @total_count = Message.count_of_blacklist @blacklist.id
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
      BlacklistedTerm.delete_all [ "blacklist_id = ?", params[:id] ]
      blacklist = Blacklist.find params[:id]
      blacklist.destroy
      flash[:notice] = "Blacklist has been deleted"
    rescue
      flash[:error] = "Could not delete blacklist"
    end
    redirect_to :action => "index"
  end

end
