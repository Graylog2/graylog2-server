class HostsController < ApplicationController
  filter_access_to :index
  filter_access_to :destroy
  filter_access_to :show
  filter_access_to :quickjump

  def index
    @hosts = Host.desc :message_count
    @hostgroups = Hostgroup.all

    @hosts_and_groups = @hosts | @hostgroups

    @host_count = Host.count
  end

  def destroy
    host = Host.find_by_host params[:id]
    if host.blank?
      flash[:error] = "Could not delete host!"
      redirect_to :action => "index"
      return
    end

    # Delete all messages of thist host.
    begin
      Message.delete_all_of_host host.host
    rescue => e
      flash[:error] = "Could not delete messages of host"
      redirect_to :action => "index"
      return
    end

    # Delete host.
    if host.destroy
      flash[:notice] = "Host has been deleted"
    else
      flash[:error] = "Could not delete host"
      redirect_to :action => "index"
      return
    end

    redirect_to :action => "index"
  end

  def quickjump
    host = Host.where(:host => params[:host].strip).first

    if host.blank?
      flash[:error] = "Unknown host"
      redirect_to :action => "index"
    else
      redirect_to host_messages_path(host.host)
    end
  end
end
