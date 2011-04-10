class HostgroupsController < ApplicationController
  filter_resource_access :additional_member => [:hosts, :settings, :rename]
#  before_filter :tabs, :except => [ :new ]

  def index
  end

  def new
    @hostgroup = Hostgroup.new
  end

  def show
    @has_sidebar = true
    @load_flot = true

    @hostgroup = Hostgroup.find_by_id params[:id]

    @messages = Message.all_of_hostgroup @hostgroup, params[:page]
    @total_count = Message.count_of_hostgroup @hostgroup
  end

  def hosts
    @load_jit = true
    @hostgroup = Hostgroup.find_by_id params[:id]
    @new_host = HostgroupHost.new

    @collected_hosts = Host.all_of_group(@hostgroup).sort_by { |h| h.host }
  end

  def settings
    @hostgroup = Hostgroup.find_by_id params[:id]
  end

  def create
    @hostgroup = Hostgroup.new(params[:hostgroup])
    if @hostgroup.save
      redirect_to hosts_path
      flash[:notice] = "Host group has been created."
    else
      flash[:error]  = "Could not create host group."
      render :controller => 'hosts', :action => 'new'
    end
  end

  def rename
    group = Hostgroup.find_by_id params[:id]
    group.name = params[:name]

    if group.save
      flash[:notice] = "Host group has been renamed."
    else
      flash[:error] = "Could not rename host group."
    end

    redirect_to :controller => "hostgroups", :action => "settings", :id => params[:id]
  end

  def destroy
    hostgroup = Hostgroup.find_by_id params[:id]

    if hostgroup.destroy
      flash[:notice] = "Hostgroup has been deleted."
    else
      flash[:error] = "Could not delete hostgroup!"
    end

    redirect_to hosts_path
  end

  protected
  def load_hostgroup
    @hostgroup = Hostgroup.find_by_id(params["id"])
  end
end
