class HostsController < ApplicationController
  filter_access_to :index
  filter_access_to :destroy
  filter_access_to :show
  filter_access_to :quickjump
  filter_access_to :showrange
  
  ignore_session_on_json :index


  def index
    @all_hosts = Host.all
    @hosts = Host.asc(:host).page(params[:page]) # all hosts, sorted alphabetically by hostname, paginated
    @host_count = Host.count
    respond_to do |format|
        format.html
        format.json { render :json => @hosts }
    end
  end

  def showrange
    @host = Host.where(:_id => BSON::ObjectId(params[:id])).first
    @has_sidebar = true
    @load_flot = true

    begin
      @from = Time.at(params[:from].to_i-Time.now.utc_offset)
      @to = Time.at(params[:to].to_i-Time.now.utc_offset)
    rescue
      flash[:error] = "Missing or invalid range parameters."
    end

    @messages = MessageGateway.all_in_range(params[:page], @from.to_i, @to.to_i, :hostname => @host.host)
    @total_count = @messages.total_result_count
  end

  def destroy
    host = Host.where(:_id => BSON::ObjectId(params[:id])).first

    if !host.nil? and host.delete
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
