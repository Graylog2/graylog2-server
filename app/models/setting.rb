class Setting < ActiveRecord::Base
  TYPE_MESSAGE_LENGTH = 1
  TYPE_MESSAGE_LENGTH_STANDARD = 150

  TYPE_MESSAGE_COUNT_INTERVAL = 2
  TYPE_MESSAGE_COUNT_INTERVAL_STANDARD = 10

  TYPE_MESSAGE_MAX_COUNT = 3
  TYPE_MESSAGE_MAX_COUNT_STANDARD = 100

  def self.get_message_length current_user
    setting = Setting.find_by_user_id_and_setting_type current_user.object_id, TYPE_MESSAGE_LENGTH
    return TYPE_MESSAGE_LENGTH_STANDARD if setting.blank?
    return setting.value
  end

  def self.get_message_count_interval current_user
    setting = Setting.find_by_user_id_and_setting_type current_user.object_id, TYPE_MESSAGE_COUNT_INTERVAL
    return TYPE_MESSAGE_COUNT_INTERVAL_STANDARD if setting.blank?
    return setting.value
  end

  def self.get_message_max_count current_user
    setting = Setting.find_by_user_id_and_setting_type current_user.object_sid, TYPE_MESSAGE_MAX_COUNT
    return TYPE_MESSAGE_MAX_COUNT_STANDARD if setting.blank?
    return setting.value
  end
end
