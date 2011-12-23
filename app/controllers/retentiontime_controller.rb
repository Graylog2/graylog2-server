class RetentiontimeController < ApplicationController
  filter_access_to :index
  filter_access_to :update

  def index
    @has_settings_tabs = true
    @retention_time = Setting.get_retention_time_days(current_user)
    @retention_frequency = Setting.get_retention_frequency_minutes(current_user)

    @last_run = ServerValue.message_retention_last_performed
  end

end
