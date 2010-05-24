module NewRelic
  module CollectionHelper
  # Transform parameter hash into a hash whose values are strictly
  # strings
  def normalize_params(params)
    case params
      when Symbol, FalseClass, TrueClass, nil
        params
      when Numeric
        truncate(params.to_s)
      when String
        truncate(params)
      when Hash
        new_params = {}
        params.each do | key, value |
          new_params[truncate(normalize_params(key),32)] = normalize_params(value)
        end
        new_params
      when Array
        params.first(20).map{|item| normalize_params(item)}
    else
      truncate(flatten(params))
    end
  end
  
  # Return an array of strings (backtrace), cleaned up for readability
  # Return nil if there is no backtrace
  
  def strip_nr_from_backtrace(backtrace)
    if backtrace
      # this is for 1.9.1, where strings no longer have Enumerable
      backtrace = backtrace.split("\n") if String === backtrace
      # strip newrelic from the trace
      backtrace = backtrace.reject {|line| line =~ /new_relic\/agent\// }
      # rename methods back to their original state
      # GJV - 4/6/10 - adding .to_s call since we were seeing line as a Fixnum in some cases
      backtrace = backtrace.collect {|line| line.to_s.gsub(/_without_(newrelic|trace)/, "")}
    end
    backtrace
  end
  
  private
  
  # Convert any kind of object to a short string.
  def flatten(object)
    s = case object 
      when nil then ''
      when object.instance_of?(String) then object
      when String then String.new(object)  # convert string subclasses to strings
      else "#<#{object.class.to_s}>"
    end
  end
  def truncate(string, len=256)
    case string
    when Symbol then string
    when nil then ""
    when String
      real_string = flatten(string)
      if real_string.size > len
        real_string = real_string.slice(0...len)
        real_string << "..."
      end
      real_string
    else
      truncate(flatten(string), len)     
    end
  end
  end
end
