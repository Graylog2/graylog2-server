class Setting < ActiveRecord::Base
  TYPE_MESSAGE_LENGTH = 1
  TYPE_MESSAGE_LENGTH_STANDARD = 150

  def self.get_message_length current_user
    setting = Setting.find_by_user_id_and_setting_type current_user.id, TYPE_MESSAGE_LENGTH
    return TYPE_MESSAGE_LENGTH_STANDARD if setting.blank?
    return setting.value
  end
end
