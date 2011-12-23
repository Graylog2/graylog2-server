class Setting
  include Mongoid::Document

  field :user_id, :type => BSON::ObjectId
  field :setting_type, :type => Integer
  field :value, :type => Integer

  TYPE_MESSAGE_LENGTH = 1
  TYPE_MESSAGE_LENGTH_STANDARD = 150

  TYPE_MESSAGE_COUNT_INTERVAL = 2
  TYPE_MESSAGE_COUNT_INTERVAL_STANDARD = 10

  TYPE_MESSAGE_MAX_COUNT = 3
  TYPE_MESSAGE_MAX_COUNT_STANDARD = 100

  TYPE_LIVETAIL_ROW_COUNT = 4
  TYPE_LIVETAIL_ROW_COUNT_STANDARD = 200

  TYPE_RETENTION_TIME_DAYS = 5
  TYPE_RETENTION_TIME_DAYS_STANDARD = 60

  TYPE_RETENTION_FREQ_MINUTES = 6
  TYPE_RETENTION_FREQ_MINUTES_STANDARD = 30

  def self.retentiontime_types
    [ TYPE_RETENTION_TIME_DAYS, TYPE_RETENTION_FREQ_MINUTES ]
  end

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

  def self.get_message_max_count current_user
    setting = Setting.where(:user_id => current_user.id, :setting_type => TYPE_MESSAGE_MAX_COUNT).first
    return TYPE_MESSAGE_MAX_COUNT_STANDARD if setting.blank?
    return setting.value
  end

  def self.get_livetail_row_count current_user
    setting = Setting.where(:user_id => current_user.id, :setting_type => TYPE_LIVETAIL_ROW_COUNT).first
    return TYPE_LIVETAIL_ROW_COUNT_STANDARD if setting.blank?
    return setting.value
  end

  def self.get_retention_time_days current_user
    setting = Setting.where(:user_id => current_user.id, :setting_type => TYPE_RETENTION_TIME_DAYS).first
    return TYPE_RETENTION_TIME_DAYS_STANDARD if setting.blank?
    return setting.value
  end
  
  def self.get_retention_frequency_minutes current_user
    setting = Setting.where(:user_id => current_user.id, :setting_type => TYPE_RETENTION_FREQ_MINUTES).first
    return TYPE_RETENTION_FREQ_MINUTES_STANDARD if setting.blank?
    return setting.value
  end

end
