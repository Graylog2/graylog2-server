class Setting
  include Mongoid::Document

  field :user_id, :type => BSON::ObjectId
  field :setting_type, :type => Integer
  field :value, :type => Object

  TYPE_MESSAGE_LENGTH = 1
  TYPE_MESSAGE_LENGTH_STANDARD = 150

  TYPE_MESSAGE_COUNT_INTERVAL = 2
  TYPE_MESSAGE_COUNT_INTERVAL_STANDARD = 10

  TYPE_ADDITIONAL_COLUMNS = 7
  TYPE_ADDITIONAL_COLUMNS_STANDARD = []

  def self.get_message_length current_user
    setting = Setting.where(:user_id => current_user.id, :setting_type => TYPE_MESSAGE_LENGTH).first
    return TYPE_MESSAGE_LENGTH_STANDARD if setting.blank?
    return setting.value
  end

  def self.get_message_count_interval current_user
    setting = Setting.where(:user_id => current_user.id, :setting_type => TYPE_MESSAGE_COUNT_INTERVAL).first
    return TYPE_MESSAGE_COUNT_INTERVAL_STANDARD if setting.blank?
    return setting.value
  end

  def self.get_additional_columns current_user
    setting = Setting.where(:user_id => current_user.id, :setting_type => TYPE_ADDITIONAL_COLUMNS).first
    return TYPE_ADDITIONAL_COLUMNS_STANDARD if setting.blank?
    return setting.value
  end

end
