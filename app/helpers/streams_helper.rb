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
    if rule.rule_type == Streamrule::TYPE_SEVERITY
        value = "#{syslog_level_to_human(rule.value)} (#{h(rule.value.to_i)})"
    end

    return "<span class=\"black\">#{type}</span>: <i>#{value}</i>"
  end

  def tabs
    @tabs = []
    if @stream
      @tabs.push ["Show", stream_path(@stream)] if permitted_to?(:show, @stream)
      @tabs.push ["Rules", rules_stream_path(@stream)] if permitted_to?(:rules, @stream)
      @tabs.push ["Forwarders", forward_stream_path(@stream)] if permitted_to?(:forward, @stream)
      @tabs.push ["Analytics", analytics_stream_path(@stream)] if permitted_to?(:analytics, @stream)
      @tabs.push ["Settings", settings_stream_path(@stream)] if permitted_to?(:show, @stream)
    end

    @tabs
  end

  def forwarder_details(forwarder)
    return String.new if forwarder.endpoint_type.blank?

    case forwarder.endpoint_type.to_sym
      when :syslog then
        return "#{forwarder.host}:#{forwarder.port}"
      when :gelf then
        return "#{forwarder.host}:#{forwarder.port}"
      when :loggly then
        return forwarder.host
      else return String.new
    end
  end
end
