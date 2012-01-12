class HealthController < ApplicationController
  filter_access_to :index

  def index
    @load_flot = true
  end

  def currentthroughput
    render :js => { :count => ServerValue.throughput[:current] }.to_json
  end
  
  def currentmqsize
    render :js => { :count => ServerValue.message_queue_current_size }.to_json
  end
end
