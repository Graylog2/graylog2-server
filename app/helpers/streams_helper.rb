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

    return "#{type}: <i>#{value}</i>"
  end

  def stream_tabs
    tabs = []
    if @stream
      tabs << ["Show", stream_path(@stream)] if permitted_to?(:show, @stream)
      tabs << ["Rules", rules_stream_path(@stream)] if permitted_to?(:rules, @stream)
      tabs << ["Analytics", analytics_stream_path(@stream)] if permitted_to?(:analytics, @stream)
      tabs << ["Settings", settings_stream_path(@stream)] if permitted_to?(:settings, @stream)
      tabs << ["Alarms", alarms_stream_path(@stream)] if permitted_to?(:alarms, @stream)
      tabs << ["Outputs", outputs_stream_path(@stream)] if permitted_to?(:outputs, @stream)
    end

    tabs
  end

  def streamrule_input_fields(value = nil, style = "display: none;", disabled = true, class_prefix = "stream-value")
    r = {}

    r[Streamrule::TYPE_MESSAGE] = text_field_tag("streamrule[value]", value, :class => "#{class_prefix}-field #{class_prefix}-message")
    r[Streamrule::TYPE_FACILITY] = text_field_tag("streamrule[value]", value, :class => "#{class_prefix}-field #{class_prefix}-facility", :style => style, :disabled => disabled)
    r[Streamrule::TYPE_HOST_REGEX] = text_field_tag("streamrule[value]", value, :class => "#{class_prefix}-field #{class_prefix}-host-regex", :style => style, :disabled => disabled)
    r[Streamrule::TYPE_FILENAME_LINE] = text_field_tag("streamrule[value]", value, :class => "#{class_prefix}-field #{class_prefix}-filename", :style => style, :disabled => disabled)
    r[Streamrule::TYPE_FULL_MESSAGE] = text_field_tag("streamrule[value]", value, :class => "#{class_prefix}-field #{class_prefix}-fullmessage", :style => style, :disabled => disabled)

    r[Streamrule::TYPE_SEVERITY] = select_tag("streamrule[value]", options_for_select(get_ordered_severities_for_select, value), :disabled => disabled, :style => style, :class => "#{class_prefix}-field #{class_prefix}-severity") 
    r[Streamrule::TYPE_SEVERITY_OR_HIGHER] = select_tag("streamrule[value]", options_for_select(get_ordered_severities_for_select, value), :disabled => disabled, :style => style, :class => "#{class_prefix}-field #{class_prefix}-severity-or-higher")
    r[Streamrule::TYPE_HOST] = select_tag("streamrule[value]", options_for_select(Host.all.collect {|host| [ h(host.host) ]}.sort, value), :disabled => disabled, :style => style, :class => "#{class_prefix}-field #{class_prefix}-host" ) 
    r[Streamrule::TYPE_ADDITIONAL] = text_field_tag("streamrule[value]", value, :class => "#{class_prefix}-field #{class_prefix}-additional-field", :disabled => disabled, :style => style)
    r[Streamrule::TYPE_FACILITY_REGEX] = text_field_tag("streamrule[value]", value, :class => "#{class_prefix}-field #{class_prefix}-facility-regex", :disabled => disabled, :style => style)

    return r
  end

  def streamrule_input_fields_for_add_form
    r = ""
    streamrule_input_fields.each do |k,v|
      r += v
    end

    return r.html_safe
  end

end
