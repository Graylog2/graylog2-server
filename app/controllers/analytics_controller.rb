class AnalyticsController < ApplicationController
  filter_access_to :index

  def index
    @has_shell = true
  end

end
