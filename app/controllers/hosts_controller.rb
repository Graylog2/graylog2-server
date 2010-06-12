class HostsController < ApplicationController
  def index
    @hosts = Host.all :order => "message_count DESC"
    @host_count = Host.count
  end

  def show
    @host = Host.find_by_host params[:id]
    @messages = Message.all_of_host params[:id], params[:page]

    if @host.blank?
      flash[:error] = "<strong>Unknown host</strong> <span>Could not find host</span>"
      redirect_to :action => "index"
    end

    @first_message = Message.first :conditions => { "host" => params[:id] }, :order => "created_at DESC"
    @last_message = Message.last :conditions => { "host" => params[:id] }, :order => "created_at DESC"
  end

  def destroy
    host = Host.find_by_host params[:id]
    if host.blank?
      flash[:error] = "<strong>Could not delete host</strong> <span>Could not find host</span>"
      redirect_to :action => "index"
      return
    end

    # Delete all messages of thist host.
    begin
      Message.delete_all_of_host params[:id]
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
    @host = Host.find_by_host params[:host]

    if @host.blank?
      flash[:error] = "<strong>Unknown host</strong> <span>Could not find host</span>"
      redirect_to :action => "index"
    else
      redirect_to :action => "show", :id => params[:host]
    end
  end
end
