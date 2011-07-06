class InvalidSelectorException < RuntimeError
end
class InvalidOperatorException < RuntimeError
end
class InvalidOptionException < RuntimeError
end
class MissingDistinctTargetException < RuntimeError
end
class MissingStreamTargetException < RuntimeError
end
class UnknownStreamException < RuntimeError
end

class Shell

  ALLOWED_SELECTORS = %w(all stream streams)
  ALLOWED_OPERATORS = %w(count find distinct)
  ALLOWED_CONDITIONALS = %w(>= <= > < = !=)
  ALLOWED_OPTIONS = %w(limit offset query)

  MAX_LIMIT = 500

  attr_reader :command, :selector, :operator, :operator_options, :distinct_target, :stream_narrows, :result, :mongo_selector

  def initialize(cmd)
    @command = cmd.strip

    parse
  end

  def compute
    case @operator
      when "count" then perform_count
      when "find" then perform_find
      when "distinct" then perform_distinct
      else raise InvalidOperatorException
    end

    @mongo_selector = criteria.selector

    return {
      :operation => @operator,
      :result => @result,
      :operator_options => @operator_options,
      :mongo_selector => @mongo_selector
    }
  end

  private
  def parse
    parse_selector
    parse_operator
    parse_operator_options

    validate

    if selector == "stream" or selector == "streams"
      parse_stream_narrows
    end

  end

  def parse_selector
    return @selector = @command.scan(/^(.+?)(\.|\()/)[0][0]
  end

  def parse_operator
    return @operator = @command.scan(/\.(.+?)\(/)[0][0]
  end

  def parse_stream_narrows
    string = @command.scan(/^streams?\((.+?)\)/)[0][0]

    # Detect empty stream selector. Result would look like this: ).find(
    raise MissingStreamTargetException if string.start_with?(').')

    streams = string.split(",")
    parsed = Array.new

    streams.each do |stream|
      stream = stream.strip

      if stream.length < 24 # shortnames are limited to less than 24 chars
        s = Stream.where(:shortname => stream).first
      else
        s = Stream.find_by_id(stream)
      end

      raise UnknownStreamException if s.blank?

      parsed << s.id.to_s
    end

    @stream_narrows = parsed

    raise MissingStreamTargetException if @stream_narrows.blank?
  rescue Mongoid::Errors::DocumentNotFound
    raise UnknownStreamException
  rescue NoMethodError
    raise MissingStreamTargetException
  end

  def parse_operator_options
    string = @command.scan(/\.(#{ALLOWED_OPERATORS.join('|')})\((.+)\)/)

    if string.blank?
      return Array.new
    end

    string = string[0][1]
    singles = string.split(",")
    parsed = Hash.new
    singles.each do |single|
      if single.start_with?("{") and single.end_with?("}")
        # This is the distinct target.
        @distinct_target = single[1..-2].strip
        next
      end

      key = single.scan(/^(.+?)(\s|#{ALLOWED_CONDITIONALS.join('|')})/)[0][0].strip
      p_value = single.scan(/(#{ALLOWED_CONDITIONALS.join('|')})(.+)$/)
      value = { :value => typify_value(p_value[0][1].strip), :condition => p_value[0][0].strip }

      # Avoid overwriting of same keys. Example (_http_return_code >= 200, _http_return_code < 300)
      if parsed[key].blank?
        # No double assignment.
        parsed[key] = value
      else
        if parsed[key].is_a?(Array)
          parsed[key] << value
        else
          parsed[key] = [ parsed[key], value ]
        end
      end
    end

    @operator_options = parsed
  rescue => e
    Rails.logger.error "Could not parse operator options: #{e.message + e.backtrace.join("\n")}"
    raise InvalidOperatorException
  end

  def typify_value(option)
    if option.start_with?('"') and option.end_with?('"')
      return option[1..-2]
    elsif option.start_with?("/") and option.end_with?("/")
      # lol, regex
      return /#{option[1..-2]}/
    else
      return option.to_i
    end
  rescue
    return String.new
  end

  def mongofy_options(options)
    criteria = Hash.new
    unless options.blank?
      options.each do |k,v|
        criteria[k] = mongo_conditionize(v)
      end
    end

    return criteria
  end

  def mongofy_stream_narrows(streams)
    return nil if streams.blank?

    criteria = Hash.new

    if streams.count == 1
      criteria = { :streams => BSON::ObjectId(streams[0]) }
    else
      stream_arr = Array.new
      streams.each do |stream|
        stream_arr << BSON::ObjectId(stream)
      end

      criteria = { :streams => { "$in" => stream_arr } }
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
      conditions = Hash.new
      v.each do |condition|
        # Return if there is a = condition as this can't be combined with other conditions.
        if condition[:condition] == "="
          return condition[:value] # No special mongo treatment for = needed.
        elsif condition[:condition] == "!=" # This needs special treatment with $nin mongo operator.
          conditions["$nin"] = Array.new if conditions["$nin"].blank?
          conditions["$nin"] << condition[:value]
        else
          conditions[map_mongo_condition(condition[:condition])] = condition[:value]
        end
      end

      return conditions
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

  def criteria
    Message.not_deleted.where(mongofy_options(@operator_options)).where(mongofy_stream_narrows(@stream_narrows))
  end

  def perform_count
    @result = criteria.count
  end

  def perform_find
    @result = criteria.order_by({"created_at" => "-1"}).limit(MAX_LIMIT).all.to_a
  end

  def perform_distinct
    if @distinct_target.blank?
      raise MissingDistinctTargetException
    end

    @result = criteria.distinct(@distinct_target.to_sym)
  end

end
