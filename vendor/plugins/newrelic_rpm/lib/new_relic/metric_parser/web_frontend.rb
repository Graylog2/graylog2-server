# The metric where the mongrel queue time is stored

class NewRelic::MetricParser::WebFrontend < NewRelic::MetricParser
  def short_name
    if segments.last == 'Average Queue Time'
      'Mongrel Average Queue Time'
    else
      super
    end
  end
  def legend_name
    'Mongrel Wait'
  end
end