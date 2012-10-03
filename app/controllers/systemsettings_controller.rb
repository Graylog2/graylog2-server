class SystemsettingsController < ApplicationController
	filter_access_to :all

	def index
		@has_settings_tabs = true
		@allow_usage_stats = SystemSetting.allow_usage_stats?
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

end