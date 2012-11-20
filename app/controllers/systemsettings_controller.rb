class SystemsettingsController < ApplicationController
  filter_access_to :all

  def index
    @has_settings_tabs = true
    @allow_usage_stats = SystemSetting.allow_usage_stats?
    @alarm_callbacks = AlarmCallback.all
    @message_outputs = MessageOutput.all_non_standard
  end

  def allow_usage_stats
    if !params[:allow].blank? and params[:allow] == "allow"
      SystemSetting.set_allow_usage_stats(true)
    else
      SystemSetting.set_allow_usage_stats(false)
    end

    SystemSetting.set_show_first_login_modal(false)

    if !params[:back_to_overview].nil?
      redirect_to messages_path
    else
      redirect_to systemsettings_path
    end
  end

  def toggle_alarmcallback_force
    if params[:typeclass].blank?
      render :status => 400, :text => "Missing parameter: typeclass"
      return
    end

    SystemSetting.set_alarm_callback_forced(params[:typeclass], !SystemSetting.alarm_callback_forced?(params[:typeclass]))
    render :status => 200, :text => ""
  end

end