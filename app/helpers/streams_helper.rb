module StreamsHelper
  def streamrule_type_to_human type
    case type
      when Streamrule::TYPE_MESSAGE:
        return "Message"
      when Streamrule::TYPE_HOST:
        return "Host"
      when Streamrule::TYPE_SEVERITY:
        return "Severity"
      when Streamrule::TYPE_FACILITY:
        return "Facility"
    end
    return "Invalid Rule"
  end

  def streamrule_to_human rule
    type = streamrule_type_to_human rule.rule_type
    value = h(rule.value)

    # Add human readable value type for SEVERITY and FACILITY.
    case rule.rule_type
      when Streamrule::TYPE_SEVERITY:
        value = "#{syslog_level_to_human(rule.value)} (#{h(rule.value.to_i)})"
      when Streamrule::TYPE_FACILITY:
        value = "#{syslog_facility_to_human(rule.value)} (#{h(rule.value.to_i)})"
    end

    return "<span class=\"black\">#{type}</span>: <i>#{value}</i>"
  end
end
