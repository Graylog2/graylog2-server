class HealthController < ApplicationController
  filter_access_to :index

  def index
    # Delete outdated server values from instances not running anymore.
    ServerValue.delete_outdated
    
    sort = params[:sort].blank? ? :timestamp : params[:sort].to_sym
    order = params[:order].blank? ? :desc : params[:order].to_sym	
 	  @server_activities = ServerActivity.all.order_by([[sort, order]]).page(params[:page])

 	  @index_information = DeflectorInformation.first
    @recent_index = @index_information.recent_index
    @indices = @index_information.indices.sort_by { |k,v| k.split("_").last.to_i }.reverse
    @nodes = DeflectorInformation.get_nodes
  end

  def currentthroughput
    render :js => { :count => Cluster.throughput }.to_json
  end

end
