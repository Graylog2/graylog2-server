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
  TYPE_SEVERITY_OR_HIGHER = 8
  TYPE_HOST_REGEX = 9
  TYPE_FULL_MESSAGE = 10
  TYPE_FILENAME_LINE = 11
  TYPE_FACILITY_REGEX = 12

  # All rules with these types will be checked for valid regex syntax.
  def regex_rules
    [ TYPE_MESSAGE, TYPE_FULL_MESSAGE, TYPE_HOST_REGEX, TYPE_FILENAME_LINE, TYPE_FACILITY_REGEX ]
  end

  def self.rule_names
    {
      self::TYPE_MESSAGE => "Short Message (regex)",
      self::TYPE_FULL_MESSAGE => "Full Message (regex)",
      self::TYPE_HOST => "Host",
      self::TYPE_HOST_REGEX => "Host (regex)",
      self::TYPE_SEVERITY => "Level",
      self::TYPE_SEVERITY_OR_HIGHER => "Level (or higher)",
      self::TYPE_FACILITY => "Facility",
      self::TYPE_FACILITY_REGEX => "Facility (regex)",
      self::TYPE_FILENAME_LINE => "Filename and line (regex)",
      self::TYPE_ADDITIONAL => "Additional field",
    }
  end

  def self.get_types_for_select_options(special = nil)
    options = Hash.new
    self.rule_names.each do |k,v|
      options[v] = k
    end
    return options
  end

  private

  def valid_regex
    return unless self.regex_rules.include?(rule_type)

    begin
      String.new =~ /#{value}/
    rescue RegexpError
      errors.add(:value, "invalid regular expression")
    end
  end
end
