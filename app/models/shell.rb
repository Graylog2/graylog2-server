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
  ALLOWED_OPERATORS = %w(count find distribution)
  RANGE_QUERY_CONDITIONALS = %w(>= <= > <)
  BOOLEAN_QUERY_CONDITIONALS = %w(= !=)
  ALLOWED_CONDITIONALS = RANGE_QUERY_CONDITIONALS | BOOLEAN_QUERY_CONDITIONALS

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
    else
      return option.to_i
    end
  rescue
    return String.new
  end

  # https://gist.github.com/995045
  def elastify_options(options)
    # options.inspect: {"host"=>{:value=>"example.org", :condition=>"!="}, "_http_response_code"=>[{:value=>300, :condition=>"<"}, {:value=>200, :condition=>">="}]}

    range_queries = extract_range_queries(options)
    bool_queries = extract_boolean_queries(options)
    
    # range_queries.inspect: [{"_http_response_code"=>{:value=>300, :condition=>"<"}}, {"_http_response_code"=>{:value=>200, :condition=>">="}}]
    # bool_queries.inspect:  {:equal=>[["message", "OHAI thar"]], :not_equal=>[]}

    criteria = Hash.new
    range_criterias = Array.new
    elastic_conditionize_ranges(range_queries).each do |range|
      range_criterias << { :range => range }
    end
      
    criteria[:query] = {
      :bool => {
        :must => Array.new,
        :must_not => Array.new
      }
    }

    # Add boolean criterias.
    unless bool_queries[:equal].blank? and bool_queries[:not_equal].blank?
      bool_queries[:equal].each do |q|
        criteria[:query][:bool][:must] << { decide_text_or_term(q[0]) => { q[0] => q[1]} }
      end

      bool_queries[:not_equal].each do |q|
        criteria[:query][:bool][:must_not] << { decide_text_or_term(q[0]) => { q[0] => q[1]} }
      end
    end
    
    # Add range criterias to boolean queries.
    unless range_criterias.blank?
      criteria[:query][:bool][:must] << range_criterias unless criteria[:query].nil? or criteria[:query][:bool].nil? or criteria[:query][:bool][:must].nil?
    end
    # Set query to match all if no query options were passed to the operator. MUST NOT overwrite stream narrows.
    if @stream_narrows.blank? and range_queries.blank? and bool_queries[:equal].blank? and bool_queries[:not_equal].blank?
      criteria[:query] = { :match_all => Hash.new }
    end

    return criteria
  end

  def decide_text_or_term(field)
    return :text if field == "message" or field == "full_message"
    return :term
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
  
    # normalized.inspect: {"_http_response_code"=>[{:value=>300, :condition=>"<"}, {:value=>200, :condition=>">="}], "_something"=>[{:value=>50, :condition=>">"}]}
    normalized.each do |field, conditions|
      cs = Hash.new
      conditions.each do |condition|
        cs[map_elastic_condition(condition[:condition])] = condition[:value]
      end

      result << { field => cs }
    end

    # result.inspect: {"_http_response_code"=>[{:value=>300, :condition=>"<"}, {:value=>200, :condition=>">="}], "_something"=>[{:value=>50, :condition=>">"}]}
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
  def extract_boolean_queries(options)
    bool_queries = { :equal => [], :not_equal => [] }
    return bool_queries if options.blank?

    options.each do |k,v|
      # XXX refactor
      if v.is_a?(Hash)
        next if !BOOLEAN_QUERY_CONDITIONALS.include?(v[:condition])
        if v[:condition] == "!="
          bool_queries[:not_equal] << [ k, v[:value] ]
        else
          bool_queries[:equal] << [ k, v[:value] ]
        end
      elsif v.is_a?(Array)
        v.each do |option|
          next if !BOOLEAN_QUERY_CONDITIONALS.include?(option[:condition])
          if option[:condition] == "!="
            bool_queries[:not_equal] << [ k, option[:value] ]
          else
            bool_queries[:equal] << [ k, option[:value] ]
          end
        end
      end
    end
    
    return bool_queries
  end

  def elastify_stream_narrows(streams)
    return Hash.new if streams.blank?

    # XXX map
    arr = Array.new
    streams.each do |stream|
      arr << BSON::ObjectId(stream)
    end

    { 
      :terms => {
        :streams => arr
      }
    }
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
    crit = elastify_options(@operator_options)
  
    narrows = elastify_stream_narrows(@stream_narrows)
    
    # add stream narrows
    unless narrows.blank? or crit[:query].nil? or crit[:query][:bool].nil?
      if crit[:query][:bool][:must].nil?
        crit[:query][:bool][:must] = [ narrows ]
      else
        crit[:query][:bool][:must] << narrows
      end
    end
    
    return crit
  end

  def perform_count
    q = criteria
    @query_hash = q
    @result = MessageGateway.dynamic_search(q).total_result_count
  end

  def perform_find
    q = criteria.merge(:size => MAX_LIMIT)
    @query_hash = q
    @result = MessageGateway.dynamic_search(q)
  end

  def perform_distribution
    if @distinct_target.blank?
      raise MissingDistinctTargetException
    end

    q = criteria
    @result = Array.new
    @query_hash = q
    MessageGateway.dynamic_distribution(@distinct_target, q).each do |r|
      @result << { :distinct => r[:distinct], :count => r[:count] }
    end

    @result.sort! { |a,b| b[:count] <=> a[:count] }
  end

end
