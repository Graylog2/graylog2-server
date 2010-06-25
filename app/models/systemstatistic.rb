class Systemstatistic
  include MongoMapper::Document

  set_database_name 'graylog2'

  key :handled_syslog_events, Float

  def self.get_total_messages
    Message.count.to_i
  end

  def self.get_message_count_of_last_minute
    begin
      return Systemstatistic.first(:order => "created_at DESC").handled_syslog_events.to_i
    rescue
      return 0
    end
  end
end
