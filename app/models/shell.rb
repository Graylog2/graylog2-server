class InvalidSelectorException < RuntimeError
end
class InvalidOperatorException < RuntimeError
end
class InvalidOptionException < RuntimeError
end

class Shell

  ALLOWED_SELECTORS = %w(all stream streams)
  ALLOWED_OPERATORS = %w(count find distinct)
  ALLOWED_CONDITIONALS = %w(>= <= > < = !=)

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
      key = single.scan(/^(.+?)(\s|#{ALLOWED_CONDITIONALS.join('|')})/)[0][0].strip
      p_value = single.scan(/(#{ALLOWED_CONDITIONALS.join('|')})(.+)$/)
      value = { :value => typify_value(p_value[0][1].strip), :condition => p_value[0][0].strip }

      # Avoid overwriting of same keys. Exampke (:_http_return_code >= 200, :_http_return_code < 300)
      if parsed[key].blank?
        # No double assignment.
        parsed[key] = value # XXX OVERWRITE!
      else
        if parsed[key].is_a?(Array)
          parsed[key] << value
        else
          parsed[key] = [ parsed[key], value ]
        end
      end
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
      puts "CONDITION ME: #{v.inspect}"
      criteria[k] = mongo_conditionize(v)
      puts "CONDITIONIZED: #{mongo_conditionize(v).inspect}"
    end

    return criteria
  end

  def mongo_conditionize(v)
    if v.is_a?(Hash)
      raise InvalidOptionException if !ALLOWED_CONDITIONALS.include?(v[:condition])
      
      if v[:condition] == "="
        return v[:value] # No special mongo treatment for = needed.
      else
        return { map_mongo_condition(v[:condition]) => v[:value] }
      end
    elsif v.is_a?(Array)
    
    else
      raise InvalidOptionException
    end
  end

  def map_mongo_condition(c)
    case c
      when ">=" then return "$gte"
      when "<=" then return "$lte"
      when ">" then return "$gt"
      when "<" then return "$lt"
      when "!=" then return "$ne"
      else raise InvalidOptionException
    end
  end

  def validate
    raise InvalidSelectorException unless ALLOWED_SELECTORS.include?(@selector)
    raise InvalidOperatorException unless ALLOWED_OPERATORS.include?(@operator)
  end

  def perform_count
    @result = Message.not_deleted.where(mongofy(@operator_options)).count
  end

end
