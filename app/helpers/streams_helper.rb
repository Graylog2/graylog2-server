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
end
