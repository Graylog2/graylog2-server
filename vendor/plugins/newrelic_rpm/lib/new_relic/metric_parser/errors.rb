class NewRelic::MetricParser::Errors < NewRelic::MetricParser
  def is_error?; true; end
  def short_name
    segments[2..-1].join(NewRelic::MetricParser::SEPARATOR)
  end
end