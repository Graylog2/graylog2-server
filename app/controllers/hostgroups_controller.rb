class HostgroupsController < ApplicationController
  def index
  end

  def new
    @hostgroup = Hostgroup.new
  end
  
  def show
    @hosts = Host.all
    @hostgroup = Hostgroup.find params[:id]

    @messages = Message.all_of_hostgroup @hostgroup, params[:page]
    @total_count = Message.count_of_hostgroup @hostgroup
    @last_message = Message.last :conditions => { "host" => { "$in" => @hostgroup.get_hostnames } }, :order => "created_at DESC"

    @new_host = HostgroupHost.new
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

  def destroy
    hostgroup = Hostgroup.find params[:id]
    if hostgroup.destroy
      flash[:notice] = "<strong>Hostgroup has been deleted</strong>"
    else
      flash[:error] = "<strong>Could not delete hostgroup</strong>"
    end

    redirect_to :controller => "hosts", :action => "index"
  end

end
