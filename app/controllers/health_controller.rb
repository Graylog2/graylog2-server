class HealthController < ApplicationController
  filter_access_to :index
  filter_access_to :currentthroughput

  def index
    @load_flot = true

    @used_memory = HistoricServerValue.used_memory(24*60)
  end

  def currentthroughput
    render :js => { :count => ServerValue.throughput[:current] }.to_json
  end
end
