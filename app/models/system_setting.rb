class SystemSetting
	include Mongoid::Document

	field :key, :type => String
	field :value, :type => Object

	SHOW_FIRST_LOGIN_MODAL = "show_first_login_modal"
	ALLOW_USAGE_STATS = "allow_usage_stats"

	def self.show_first_login_modal?
		val = get(SHOW_FIRST_LOGIN_MODAL)
		return true if val.nil? or val == true
		return false
	end

	def self.allow_usage_stats?
		val = get(ALLOW_USAGE_STATS)
		return false if val.nil? or val == false
		return true if val == true
		return false
	end

	def self.set_show_first_login_modal(what)
		set(SHOW_FIRST_LOGIN_MODAL, what)
	end

	def self.set_allow_usage_stats(what)
		set(ALLOW_USAGE_STATS, what)
	end

	private
	def self.get(key)
		r = where(:key => key).first
		return nil if r.nil?
		return r.value
	end

	def self.set(key, value)
		SystemSetting.delete_all(:key => key)
		s = SystemSetting.new
		s.key = key
		s.value = value
		s.save
	end

end