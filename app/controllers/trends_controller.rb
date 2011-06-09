class TrendsController < ApplicationController
  filter_access_to :index

  def index
    @load_flot = true
  end
end
