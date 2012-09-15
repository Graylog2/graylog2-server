class HealthController < ApplicationController
  filter_access_to :index

  def index
    # Delete outdated server values from instances not running anymore.
    ServerValue.delete_outdated
    
      @server_activities = ServerActivity.all.desc(:timestamp).page(params[:page])
  end

  def currentthroughput
    render :js => { :count => Cluster.throughput }.to_json
  end

end
