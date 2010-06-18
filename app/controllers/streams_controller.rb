class StreamsController < ApplicationController
  def index
    @new_stream = Stream.new
    @streams = Stream.all
  end

  def show
    @stream = Stream.find params[:id]
    @messages = Message.all_of_stream @stream.id, params[:page]
    @new_rule = Streamrule.new
  end

  def create
    new_stream = Stream.new params[:stream]
    if new_stream.save
      flash[:notice] = "Stream has been created"
    else
      flash[:error] = "Could not create stream"
    end
    redirect_to :action => "index"
  end

  def destroy
    begin
      Streamrule.delete_all [ "stream_id = ?", params[:id] ]
      stream = Stream.find params[:id]
      stream.destroy
      flash[:notice] = "Stream has been deleted"
    rescue
      flash[:error] = "Could not delete stream"
    end
    redirect_to :action => "index"
  end

  def get_hosts_statistic
    throw "Missing stream ID" if params[:id].blank?
    
    ret = String.new

    total_message_count = Stream.get_message_count(params[:id]).to_i
    hosts = Stream.get_distinct_hosts params[:id]

    ready_hosts = Array.new
    hosts.each do |host|
      message_count = Stream.get_count_by_host(params[:id], host).to_i
      # Thanks to Sarah and her wicked percentage calculation skills. (<3)
      percent = 100-(((total_message_count-message_count)*100)/total_message_count)
      ready_hosts << { 'name' => host, 'percent' => percent.to_i }
    end

    if hosts.blank?
      render :text => 'No messages found.'
      return
    end

    render :partial => 'statistics', :locals => { :hosts => ready_hosts }
  end

end
