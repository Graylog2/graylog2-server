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
  ALLOWED_OPERATORS = %w(count find distinct distribution)
  RANGE_QUERY_CONDITIONALS = %w(>= <= > <)
  TEXT_QUERY_CONDITIONALS = %w(= !=)
  ALLOWED_CONDITIONALS = RANGE_QUERY_CONDITIONALS | TEXT_QUERY_CONDITIONALS

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
      when "distribution" then perform_distribution
      else raise InvalidOperatorException
    end

    return {
      :operation => @operator,
      :result => @result,
      :operator_options => @operator_options,
      :query_hash => @query_hash
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

  def elastify_options(options)
    # options.inspect: {"host"=>{:value=>"example.org", :condition=>"!="}, "_http_response_code"=>[{:value=>300, :condition=>"<"}, {:value=>200, :condition=>">="}]}

    range_queries = extract_range_queries(options)
    text_queries = extract_text_queries(options)

    # range_queries.inspect: [{"_http_response_code"=>{:value=>300, :condition=>"<"}}, {"_http_response_code"=>{:value=>200, :condition=>">="}}]
    # text_queries.inspect:  [{"host"=>{:value=>"example.org", :condition=>"!="}}]

    criteria = Hash.new
    range_criterias = Array.new
    elastic_conditionize_ranges(range_queries).each do |range|
      range_criterias << { :range => range }
    end

    # Add range criterias.
    unless range_criterias.blank?
      criteria[:filtered] = Hash.new
      criteria[:filtered][:query] = { "match_all" => Hash.new } # possibly plug text_queries here
      criteria[:filtered][:filter] = Hash.new 
      criteria[:filtered][:filter][:and] = range_criterias 
    end

    # Set to match all, if nothing was selected before.
    criteria = { "match_all" => Hash.new } if criteria.blank?

    return { :query => criteria } 
  end

  def elastic_conditionize_ranges(ranges)
    result = Array.new
    # normalize on key first
    normalized = Hash.new
    ranges.each do |range|
      # XXX something smells here
      # range.inspect: {"_http_response_code"=>{:value=>300, :condition=>"<"}}
      range.each do |field, conditions|
        normalized[field] = Array.new if normalized[field].nil?
        normalized[field] << conditions
      end
    end
  
    # normalized.inspect: {"_http_response_code"=>[{:value=>300, :condition=>"<"}, {:value=>200, :condition=>">="}]} 
    normalized.each do |field, conditions|
      conditions.each do |condition|
        result << { field => { map_elastic_condition(condition[:condition]) => condition[:value] }}
      end
    end
   
    # XXX result.inspect: 
    return result
  end

  def extract_range_queries(options)
    range_queries = Array.new
    return range_queries if options.blank?

    options.each do |k,v|
      # XXX refactor
      if v.is_a?(Hash)
        range_queries << { k => v} if RANGE_QUERY_CONDITIONALS.include?(v[:condition])
      elsif v.is_a?(Array)
        v.each do |option|
          range_queries << { k => option } if RANGE_QUERY_CONDITIONALS.include?(option[:condition])
        end
      end
    end

    return range_queries
  end
  
  # XXX refactor: merge with range query extractor
  def extract_text_queries(options)
    text_queries = Array.new
    return text_queries if options.blank?

    options.each do |k,v|
      # XXX refactor
      if v.is_a?(Hash)
        text_queries << { k => v} if TEXT_QUERY_CONDITIONALS.include?(v[:condition])
      elsif v.is_a?(Array)
        v.each do |option|
          text_queries << { k => option } if TEXT_QUERY_CONDITIONALS.include?(option[:condition])
        end
      end
    end

    return text_queries
  end

  def elastify_stream_narrows(streams)
    return Hash.new if streams.blank?

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

  # XXX REMOVE?
  def map_elastic_condition(c)
    case c
      when ">=" then return "gte"
      when "<=" then return "lte"
      when ">" then return "gt"
      when "<" then return "lt"
      else raise InvalidOptionException
    end
  end

  def validate
    raise InvalidSelectorException unless ALLOWED_SELECTORS.include?(@selector)
    raise InvalidOperatorException unless ALLOWED_OPERATORS.include?(@operator)
  end

  def criteria
    elastify_options(@operator_options).merge(elastify_stream_narrows(@stream_narrows))
  end

  def perform_count
    @result = MessageGateway.dynamic_search(criteria).total_result_count
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

  def perform_distribution
    if @distinct_target.blank?
      raise MissingDistinctTargetException
    end

    @result = Array.new
    criteria.distinct(@distinct_target.to_sym).each do |r|
      @result << { :distinct => r, :count => criteria.where(@distinct_target.to_sym => r).count }
    end

    @result.sort! { |a,b| b[:count] <=> a[:count] }
  end

end
