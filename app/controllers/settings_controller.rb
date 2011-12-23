class SettingsController < ApplicationController
  filter_access_to :index
  filter_access_to :store

  def index
    @has_settings_tabs = true
  end

  def store
    Setting.where(:user_id => current_user.id, :setting_type => params[:setting_type]).delete_all

    setting = Setting.new
    setting.user_id = current_user.id
    setting.setting_type = params[:setting_type]
    setting.value = params[:value].to_i

    if setting.save
      flash[:notice] = "Setting has been saved!"
    else
      flash[:error] = "Could not edit setting."
    end

    if Setting.retentiontime_types.include?(params[:setting_type].to_i)
      controller = "retentiontime"
    else
      controller = "settings"
    end

    redirect_to :controller => controller, :action => "index"
  end
end
