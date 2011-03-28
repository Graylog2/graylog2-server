class HostgroupHostsController < ApplicationController
  filter_resource_access

  def create
    # Check if hostgroup exists.
    if !Hostgroup.exists?(:conditions => { "_id" => BSON::ObjectId.from_string(params[:new_host][:hostgroup_id]) })
      flash[:error] = "Group does not exist."
      redirect_to hosts_path
      return
    end

    if params[:new_host][:ruletype].to_i == HostgroupHost::TYPE_SIMPLE
      # Check if that host exists.
      if !Host.exists?(:conditions => { "host" => params[:new_host][:hostname] })
        flash[:error] = "Host does not exist!"
        back_to_hostgroup params[:new_host][:hostgroup_id]
        return
      end

      # Check if that host is already in the hostgroup.
      if HostgroupHost.exists?(:conditions => { "hostname" => params[:new_host][:hostname], "hostgroup_id" => BSON::ObjectId.from_string(params[:new_host][:hostgroup_id]) })
        flash[:error] = "Host already in group."
        back_to_hostgroup params[:new_host][:hostgroup_id]
        return
      end
    end

    host = HostgroupHost.new params[:new_host]
    host["hostgroup_id"] = BSON::ObjectId.from_string(params[:new_host][:hostgroup_id])
    if host.save
      flash[:notice] = "Added to hostgroup."
    else
      flash[:error] = "Could not add to hostgroup!"
    end

    back_to_hostgroup params[:new_host][:hostgroup_id]
  end

  def destroy
    host = HostgroupHost.find params[:id]
    if host.destroy
      flash[:notice] = "Host has been removed from hostgroup."
    else
      flash[:error] = "Could not remove host from hostgroup!"
    end

    back_to_hostgroup params[:group_id]
  end

  private
  def back_to_hostgroup id
    redirect_to :controller => "hostgroups", :action => "hosts", :id => id
  end

end
