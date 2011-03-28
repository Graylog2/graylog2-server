module StreamsHelper
  def streamrule_type_to_human type
    case type
      when Streamrule::TYPE_MESSAGE then
        return "Message"
      when Streamrule::TYPE_HOST then
        return "Host"
      when Streamrule::TYPE_SEVERITY then
        return "Severity"
      when Streamrule::TYPE_FACILITY then
        return "Facility"
      when Streamrule::TYPE_TIMEFRAME then
        return "Timeframe"
      when Streamrule::TYPE_ADDITIONAL then
        return "Additional field"
    end
    return "Invalid Rule"
  end

  def streamrule_to_human rule
    type = streamrule_type_to_human rule.rule_type
    value = h(rule.value)

    # Add human readable value type for SEVERITY and FACILITY.
    case rule.rule_type
      when Streamrule::TYPE_SEVERITY then
        value = "#{syslog_level_to_human(rule.value)} (#{h(rule.value.to_i)})"
      when Streamrule::TYPE_FACILITY then
        value = Facility.to_human(rule.value)
    end

    return "<span class=\"black\">#{type}</span>: <i>#{value}</i>"
  end
  
  def tabs
    @tabs = []
    if @stream
      @tabs.push ["Show", stream_path(@stream)] if permitted_to?(:show, @stream)
      @tabs.push ["Rules", rules_stream_path(@stream)] if permitted_to?(:rules, @stream)
      @tabs.push ["Analytics", analytics_stream_path(@stream)] if permitted_to?(:analytics, @stream)
      @tabs.push ["Settings", settings_stream_path(@stream)] if permitted_to?(:show, @stream)
    end
    
    @tabs
  end
end
