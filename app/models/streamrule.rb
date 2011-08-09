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
  TYPE_HOSTGROUP = 7
  TYPE_SEVERITY_OR_HIGHER = 8
  TYPE_HOST_REGEX = 9
  TYPE_FULL_MESSAGE = 10

  def self.get_types_for_select_options(special = nil)
    {
      "Short Message (regex)" => self::TYPE_MESSAGE,
      "Full Message (regex)" => self::TYPE_FULL_MESSAGE,
      "Host" => self::TYPE_HOST,
      "Host (regex)" => self::TYPE_HOST_REGEX,
      "Hostgroup" => self::TYPE_HOSTGROUP,
      "Severity" => self::TYPE_SEVERITY,
      "Severity (or higher)" => self::TYPE_SEVERITY_OR_HIGHER,
      "Facility" => self::TYPE_FACILITY,
      "Additional field" => self::TYPE_ADDITIONAL
    }
  end

  private

  def valid_regex
    return if [TYPE_MESSAGE, TYPE_FULL_MESSAGE].include?(rule_type)

    begin
      String.new =~ /#{value}/
    rescue RegexpError
      errors.add(:value, "invalid regular expression")
    end
  end
end
