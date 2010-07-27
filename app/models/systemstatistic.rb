class Systemstatistic
  include MongoMapper::Document

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

  def self.get_array_for_flot
    begin
      ret = String.new

      stats = Systemstatistic.all :limit => 120, :order => "created_at DESC"

      return "[]" if stats.blank?

      ret += "["
      stats.each do |stat|
        ret += "[#{(stat.created_at.to_i*1000)+(Time.now.utc_offset*1000)}, #{stat.handled_syslog_events.to_i}],"
      end
      return ret.chop + "]"
    rescue
      return "[]"
    end
  end
end
