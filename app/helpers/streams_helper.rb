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
      tabs << ["Show", stream_path(@stream)] if permitted_to?(:show, @stream)
      tabs << ["Rules", rules_stream_path(@stream)] if permitted_to?(:rules, @stream)
      tabs << ["Analytics", analytics_stream_path(@stream)] if permitted_to?(:analytics, @stream)
      tabs << ["Alarms", alarms_stream_path(@stream)] if permitted_to?(:alarms, @stream)
      tabs << ["Outputs", outputs_stream_path(@stream)] if permitted_to?(:outputs, @stream)
      tabs << ["Settings", settings_stream_path(@stream)] if permitted_to?(:settings, @stream)
    end

    tabs
  end

end
