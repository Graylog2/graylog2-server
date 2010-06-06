class Streamrule < ActiveRecord::Base
  belongs_to :stream

  TYPE_MESSAGE = 1
  TYPE_HOST = 2
  TYPE_SEVERITY = 3
  TYPE_FACILITY = 4

  def self.get_types_for_select_options
    {
      "Message" => self::TYPE_MESSAGE,
      "Host" => self::TYPE_HOST,
      "Severity" => self::TYPE_SEVERITY,
      "Facility" => self::TYPE_FACILITY
    }
  end

  def self.get_message_condition_hash stream_id = nil
    if stream_id.blank?
      return nil
    end
    
    terms = self.find_all_by_stream_id_and_rule_type stream_id, self::TYPE_MESSAGE

    return nil if terms.blank?

    conditions = Array.new
    terms.each do |term|
      conditions << /#{Regexp.escape(term.value)}/
    end

    return { "$in" => conditions }
  end

  def self.get_host_condition_hash stream_id = nil
    if stream_id.blank?
      return nil
    end

    hosts = self.find_all_by_stream_id_and_rule_type stream_id, self::TYPE_HOST

    return nil if hosts.blank?

    conditions = Array.new
    hosts.each do |host|
      conditions << host.value
    end

    return { "$in" => conditions }
  end

end
