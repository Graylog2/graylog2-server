class Streamrule
  include Mongoid::Document

  embedded_in :stream, :inverse_of => :streamrules

  validates_presence_of :rule_type
  validates_presence_of :value
  validate :valid_regex

  field :rule_type, :type => Integer
  field :value, :type => String

  TYPE_MESSAGE = 1
  TYPE_HOST = 2
  TYPE_SEVERITY = 3
  TYPE_FACILITY = 4
  TYPE_TIMEFRAME = 5
  TYPE_ADDITIONAL = 6

  def self.get_types_for_select_options(special = nil)
    {
      "Message (regex)" => self::TYPE_MESSAGE,
      "Host" => self::TYPE_HOST,
      "Severity" => self::TYPE_SEVERITY,
      "Facility" => self::TYPE_FACILITY,
      "Additional field" => self::TYPE_ADDITIONAL
    }
  end

  private

  def valid_regex
    return if rule_type != TYPE_MESSAGE

    begin
      String.new =~ /#{value}/
    rescue RegexpError
      errors.add(:value, "invalid regular expression")
    end
  end
end
