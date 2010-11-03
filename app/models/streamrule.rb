class Streamrule < ActiveRecord::Base
  belongs_to :stream

  validates_presence_of :stream_id
  validates_presence_of :rule_type
  validates_presence_of :value

  TYPE_MESSAGE = 1
  TYPE_HOST = 2
  TYPE_SEVERITY = 3
  TYPE_FACILITY = 4
  TYPE_TIMEFRAME = 5

  def self.get_types_for_select_options
    {
      "Message" => self::TYPE_MESSAGE,
      "Timeframe" => self::TYPE_TIMEFRAME,
      "Host" => self::TYPE_HOST,
      "Severity" => self::TYPE_SEVERITY,
      "Facility" => self::TYPE_FACILITY
    }
  end
  
  def to_condition
    case rule_type
    when TYPE_MESSAGE then
      return {:message => /#{Regexp.escape value}/}
    when TYPE_HOST then
      return {:host => value}
    when TYPE_SEVERITY then
      return {:level => value.to_i}
    when TYPE_FACILITY then
      return {:facility => value.to_i}
    when TYPE_TIMEFRAME then
      return {:timeframe => value}
    end
  end

  # def self.get_message_condition_array  stream_id = nil
  #   if stream_id.blank?
  #     return nil
  #   end
  #   
  #   terms = self.find_all_by_stream_id_and_rule_type stream_id, self::TYPE_MESSAGE
  # 
  #   return nil if terms.blank?
  # 
  #   conditions = Array.new
  #   terms.each do |term|
  #     conditions << /#{Regexp.escape(term.value.strip)}/
  #   end
  # 
  #   return conditions
  # end
  # 
  # def self.get_host_condition_hash stream_id = nil
  #   if stream_id.blank?
  #     return nil
  #   end
  # 
  #   hosts = self.find_all_by_stream_id_and_rule_type stream_id, self::TYPE_HOST
  # 
  #   return nil if hosts.blank?
  # 
  #   conditions = Array.new
  #   hosts.each do |host|
  #     conditions << host.value
  #   end
  # 
  #   return { "$in" => conditions }
  # end
  # 
  # def self.get_facility_condition_hash stream_id = nil
  #   if stream_id.blank?
  #     return nil
  #   end
  # 
  #   facilities = self.find_all_by_stream_id_and_rule_type stream_id, self::TYPE_FACILITY
  # 
  #   return nil if facilities.blank?
  # 
  #   conditions = Array.new
  #   facilities.each do |facility|
  #     conditions << facility.value.to_i
  #   end
  # 
  #   return { "$in" => conditions }
  # end
  # 
  # def self.get_severity_condition_hash stream_id = nil
  #   if stream_id.blank?
  #     return nil
  #   end
  # 
  #   severities = self.find_all_by_stream_id_and_rule_type stream_id, self::TYPE_SEVERITY
  # 
  #   return nil if severities.blank?
  # 
  #   conditions = Array.new
  #   severities.each do |severity|
  #     conditions << severity.value.to_i
  #   end
  # 
  #   return { "$in" => conditions }
  # end
  # 
  # def self.get_timeframe_condition_hash stream_id = nil
  #   if stream_id.blank?
  #     return nil
  #   end
  # 
  #   timeframe = self.find_all_by_stream_id_and_rule_type stream_id, self::TYPE_TIMEFRAME
  # 
  #   return nil if timeframe.blank?
  # 
  #   return Message.get_conditions_from_date(timeframe.first.value)
  # end

end
