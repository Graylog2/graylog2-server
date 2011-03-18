class Streamrule
  include Mongoid::Document
  
  #belongs_to :stream
  embedded_in :stream, :inverse_of => :streamrules

  validates_presence_of :rule_type
  validates_presence_of :value
  
  field :rule_type, :type => Integer
  field :value, :type => String

  TYPE_MESSAGE = 1
  TYPE_HOST = 2
  TYPE_SEVERITY = 3
  TYPE_FACILITY = 4
  TYPE_TIMEFRAME = 5
  TYPE_ADDITIONAL = 6

  def self.get_types_for_select_options
    {
      "Message (regex)" => self::TYPE_MESSAGE,
      "Timeframe" => self::TYPE_TIMEFRAME,
      "Host" => self::TYPE_HOST,
      "Severity" => self::TYPE_SEVERITY,
      "Facility" => self::TYPE_FACILITY,
      "Additional field" => self::TYPE_ADDITIONAL
    }
  end
  
  def to_condition(op = "$in")
    case rule_type
    when TYPE_MESSAGE then
      return {:message => /#{value}/}
    when TYPE_HOST then
      return {:host => {op => [value]}}
    when TYPE_SEVERITY then
      return {:level => {op => [value.to_i]}}
    when TYPE_FACILITY then
      return {:facility => {op => [value.to_i]}}
    when TYPE_TIMEFRAME then
      return {:created_at => Message.get_conditions_from_date(value)}
    when TYPE_ADDITIONAL then
      parts = value.split("=")
      return Hash.new if parts[0].blank? or parts[1].blank?
      return {"_#{parts[0]}".to_sym => parts[1]}
    end
  end

end
