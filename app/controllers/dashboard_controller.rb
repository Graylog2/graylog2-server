class DashboardController < ApplicationController
  filter_resource_access

  layout "dashboard"

  def index
    @messages = Message.all_with_blacklist
  end

end
