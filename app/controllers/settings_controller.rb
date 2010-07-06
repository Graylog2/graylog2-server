class SettingsController < ApplicationController
  def index
  end

  def store
    # Find out if this setting is already stored.
    setting = Setting.find_by_user_id_and_setting_type current_user.id, params[:setting_type]
    if setting.blank?
      # This setting does not exist. Create it.
      setting = Setting.new
      setting.user_id = current_user.id
      setting.setting_type = params[:setting_type]
      setting.value = params[:value].to_i
      flash[:notice] = "<strong>Setting has been saved</strong><span>Created new setting</span>"
      redirect_to :action => "index" if setting.save
      return
    else
      # Update the already existing setting.
      setting.value = params[:value].to_i
      flash[:notice] = "<strong>Setting has been saved</strong><span>Updated setting</span>"
      redirect_to :action => "index" if setting.save
      return
    end

    flash[:error] = "Could not edit setting."
    redirect_to :action => "index"
  end
end
