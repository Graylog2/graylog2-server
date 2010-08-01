class DashboardController < ApplicationController

  layout "dashboard"

  def index
    @messages = Message.all_with_blacklist
  end

end
