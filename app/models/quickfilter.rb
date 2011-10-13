class Quickfilter

  def self.extract_additional_fields_from_request(filters)
    return Hash.new if filters[:additional].blank? or filters[:additional][:keys].blank? or filters[:additional][:values].blank?

    ret = Hash.new
    i = 0
    filters[:additional][:keys].each do |key|
      next if key.blank? or filters[:additional][:values][i].blank?
      ret[key] = filters[:additional][:values][i]
      i += 1
    end

    return ret
  end
 
  # returns something like: { :greater => 100, :lower => 200}
  def self.get_conditions_timeframe(timeframe)
    conditions = Hash.new
    re = /^(from (.+)){0,1}?(to (.+))$/
    re2 = /^(from (.+))$/

    if (matches = (re.match(timeframe) or re2.match(timeframe)))
      from = matches[2]
      to = matches[4]

      conditions[:greater] = Chronic::parse(from).to_i unless from.blank?
      conditions[:lower] = Chronic::parse(to).to_i unless to.blank?
    end

    return conditions
  end

end
