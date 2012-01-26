class AdditionalcolumnsController < ApplicationController
  filter_access_to :index
  
  def index
    @has_settings_tabs = true
    @additional_columns_setting = Setting.where(:user_id => current_user.id,
        :setting_type => Setting::TYPE_ADDITIONAL_COLUMNS).first
  end
end