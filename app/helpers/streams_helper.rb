module StreamsHelper
  def streamrule_type_to_human(type)
    name = Streamrule.rule_names[type]
    name.blank? ? "Invalid rule" : name
  end

  def streamrule_to_human rule
    type = streamrule_type_to_human(rule.rule_type)
    value = CGI::escapeHTML(rule.value)

    # Add human readable value type for SEVERITY
    if rule.rule_type == Streamrule::TYPE_SEVERITY or rule.rule_type == Streamrule::TYPE_SEVERITY_OR_HIGHER
        value = "#{syslog_level_to_human(rule.value)} (#{rule.value.to_i})"
    end

    return "<span class=\"black\">#{type}</span>: <i>#{value}</i>"
  end

  def stream_tabs
    tabs = []
    if @stream
      tabs.push ["Show", stream_path(@stream)] if permitted_to?(:show, @stream)
      tabs.push ["Rules", rules_stream_path(@stream)] if permitted_to?(:rules, @stream)
      tabs.push ["Forwarders", forward_stream_path(@stream)] if permitted_to?(:forward, @stream)
      tabs.push ["Analytics", analytics_stream_path(@stream)] if permitted_to?(:analytics, @stream)
      tabs.push ["Alarms", alarms_stream_path(@stream)] if permitted_to?(:alarms, @stream)
      tabs.push ["Settings", settings_stream_path(@stream)] if permitted_to?(:settings, @stream)
    end

    tabs
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
