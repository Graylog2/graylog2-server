class DashboardController < ApplicationController
  filter_resource_access

  layout "dashboard"

  def index
    if params[:stream_id].blank?
      @messages = Message.all_with_blacklist
    else
      @messages = Message.by_stream(params[:stream_id]).all_with_blacklist
    end
  end

end
