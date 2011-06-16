class InvalidSelectorException < RuntimeError
end
class InvalidOperatorException < RuntimeError
end
class InvalidOptionException < RuntimeError
end

class Shell

  ALLOWED_SELECTORS = %w(all stream streams)
  ALLOWED_OPERATORS = %w(count find distinct)

  attr_reader :command, :selector, :operator, :operator_options, :modifiers, :result

  def initialize(cmd)
    @command = cmd

    parse
  end

  def compute
    case @operator
      when "count" then perform_count
      when "find" then perform_find
      when "distinct" then perform_distinct
      else raise InvalidOperatorException
    end

    return {
      :operation => @operator,
      :result => @result,
      :operator_options => @operator_options
    }
  end

  private
  def parse
    parse_selector
    parse_operator
    parse_operator_options

    validate
  end

  def parse_selector
    @selector = @command.scan(/^(.+?)\./)[0][0]
  end

  def parse_operator
    @operator = @command.scan(/\.(.+?)\(/)[0][0]
  end

  def parse_operator_options
    string = @command.scan(/\..+\((.+?)\)/)[0][0]
    singles = string.split(",")

    parsed = Hash.new
    singles.each do |single|
      key = single.scan(/^(.+?)(\s|=)/)[0][0].strip
      value = typify_value(single.scan(/=(.+)$/)[0][0].strip)

      parsed[key.to_s] = value
    end

    @operator_options = parsed
  rescue
    @operator_options = Array.new
  end

  def typify_value(option)
    if option.start_with?('"') and option.end_with?('"')
      return option[1..-2]
    else
      return option.to_i
    end
  rescue
    return String.new
  end

  def mongofy(options)
    criteria = Hash.new
    options.each do |k,v|
      criteria[k] = conditions(v)
    end

    return criteria
  end

  def conditions(v)
    v
  end

  def validate
    raise InvalidSelectorException unless ALLOWED_SELECTORS.include?(@selector)
    raise InvalidOperatorException unless ALLOWED_OPERATORS.include?(@operator)
  end

  def perform_count
    @result = Message.not_deleted.where(mongofy(@operator_options)).count
  end

end
