class HostgrouphostsController < ApplicationController

  def create
    # Check if that host exists.
    if Host.find_by_host(params[:new_host][:hostname]).blank?
      flash[:error] = "Host does not exist!"
      back_to_hostgroup params[:new_host][:hostgroup_id]
      return
    end

    # Check if that host is already in the hostgroup.
    unless HostgroupHost.find_by_hostname_and_hostgroup_id(params[:new_host][:hostname], params[:new_host][:hostgroup_id]).blank?
      flash[:error] = "Host already in group."
      back_to_hostgroup params[:new_host][:hostgroup_id]
      return
    end

    host = HostgroupHost.new params[:new_host]
    if host.save
      flash[:notice] = "Added host to hostgroup."
    else
      flash[:error] = "Could not add host to hostgroup!"
    end

    back_to_hostgroup params[:new_host][:hostgroup_id]
  end

  def destroy
    host = HostgroupHost.find_by_hostname_and_hostgroup_id Base64.decode64(params[:name]), params[:hostgroup]
    if host.destroy
      flash[:notice] = "Host has been removed from hostgroup."
    else
      flash[:error] = "Could not remove host from hostgroup!"
    end

    back_to_hostgroup params[:hostgroup]
  end

  private
  def back_to_hostgroup id
    redirect_to :controller => "hostgroups", :action => "show", :id => id
  end

end
