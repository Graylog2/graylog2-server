class HealthController < ApplicationController

  def index
    @load_flot = true

    @used_memory = HistoricServerValue.used_memory(24*60)
  end

  def currentthroughput
    render :text => { :count => ServerValue.throughput[:current] }.to_json
  end
end
