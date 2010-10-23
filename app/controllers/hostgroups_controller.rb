class HostgroupsController < ApplicationController
  def index
  end

  def new
    @hostgroup = Hostgroup.new
  end

  def create  
    @hostgroup = Hostgroup.new(params[:hostgroup])
    if @hostgroup.save
      redirect_to :controller => 'hosts', :action => 'index'
      flash[:notice] = "Host group has been created."
    else
      flash[:error]  = "Could not create host group."
      render :controller => 'hosts', :action => 'new'
    end
  end
end
