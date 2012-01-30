class SettingsController < ApplicationController
  filter_access_to :index
  filter_access_to :store
  filter_access_to :removecolumn

  def index
    @has_settings_tabs = true
  end

  def store
    if params[:setting_type].to_i == Setting::TYPE_ADDITIONAL_COLUMNS
      store_additional_column_setting
    else
      store_regular_setting
    end

    redirect_to :controller => select_controller, :action => "index"
  end
  
  def removecolumn
    setting = Setting.find(params[:id])
    deleted_column = setting.value.delete(params[:column])
    
    if deleted_column.nil?
      flash[:error] = "Column doesn't exist."
    elsif setting.save
      flash[:notice] = "Removed additional column."
    else
      flash[:error] = "Could not remove column."
    end
    
    redirect_to additionalcolumns_path
  end
  
  private
    
    def select_controller
      setting_type = params[:setting_type].to_i
      
      if Setting.retentiontime_types.include?(setting_type)
        controller = "retentiontime"
      elsif setting_type == Setting::TYPE_ADDITIONAL_COLUMNS
        controller = "additionalcolumns"
      else
        controller = "settings"
      end
      
      return controller
    end
    
    def store_regular_setting
      Setting.where(:user_id => current_user.id,
          :setting_type => params[:setting_type]).delete_all

      setting = create_setting(params[:setting_type], params[:value].to_i)
      save_setting(setting)
    end
    
    def store_additional_column_setting
      setting = Setting.where(:user_id => current_user.id,
          :setting_type => params[:setting_type]).first

      setting ||= create_setting(params[:setting_type],
          Setting::TYPE_ADDITIONAL_COLUMNS_STANDARD.clone)

      setting.value << params[:value]
      
      if params[:value].empty?
        flash[:error] = "Column can't be empty."
      elsif setting.value.uniq!
        flash[:error] = "Column already exists."
      else
        save_setting(setting)
      end
    end
    
    def create_setting(setting_type, value)
      setting = Setting.new
      setting.user_id = current_user.id
      setting.setting_type = setting_type
      setting.value = value
      
      return setting
    end
    
    def save_setting(setting)
      if setting.save
        flash[:notice] = "Setting has been saved!"
      else
        flash[:error] = "Could not edit setting."
      end
    end

end
