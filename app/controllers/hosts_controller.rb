class HostsController < ApplicationController
  def index
    @hosts = Host.all :order => "message_count DESC"
    @hostgroups = Hostgroup.all

    @hosts_and_groups = @hosts | @hostgroups

    @host_count = Host.count
    Graph.update_total
  end

  def show
    @host = Host.find_by_host Base64.decode64(params[:id])
    @messages = Message.all_of_host @host.host, params[:page]
    @total_count = Message.count_of_host @host.host

    if @host.blank?
      flash[:error] = "<strong>Unknown host</strong> <span>Could not find host</span>"
      redirect_to :action => "index"
    end

    @last_message = Message.last :conditions => { "host" => @host.host }, :order => "created_at DESC"
  end

  def destroy
    host = Host.find_by_host Base64.decode64(params[:id])
    if host.blank?
      flash[:error] = "<strong>Could not delete host</strong> <span>Could not find host</span>"
      redirect_to :action => "index"
      return
    end

    # Delete all messages of thist host.
    begin
      Message.delete_all_of_host host.host
    rescue => e
      flash[:error] = "<strong>Could not delete host</strong> <span>Could not delete messages of host</span>"
      redirect_to :action => "index"
      return
    end

    # Delete host.
    if host.destroy
      flash[:notice] = "<strong>Host has been deleted</strong>"
    else
      flash[:error] = "<strong>Could not delete host</strong>"
      redirect_to :action => "index"
      return
    end

    redirect_to :action => "index"
  end

  def quickjump
    @host = Host.find_by_host params[:host].strip

    if @host.blank?
      flash[:error] = "<strong>Unknown host</strong> <span>Could not find host</span>"
      redirect_to :action => "index"
    else
      redirect_to :action => "show", :id => Base64.encode64(params[:host].strip)
    end
  end
end
