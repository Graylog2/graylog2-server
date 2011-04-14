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
    host = Host.where(:_id => BSON::ObjectId(params[:id])).first

    # Delete all messages of thist host.
    Message.where(:host => host.host).update_all(
      :deleted => true
    )

    # Delete host.
    if host.delete
      flash[:notice] = "Host has been deleted"
    else
      flash[:error] = "Could not delete host"
    end

    redirect_to hosts_path
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
