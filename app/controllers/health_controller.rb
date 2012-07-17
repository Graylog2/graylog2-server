class HealthController < ApplicationController
  filter_access_to :index

  def index
    @load_flot = true
  end

  def currentthroughput
    render :js => { :count => Cluster.throughput }.to_json
  end

end
