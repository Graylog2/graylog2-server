# OtherTransaction metrics must have at least three segments: /OtherTransaction/<task>/*

class NewRelic::MetricParser::OtherTransaction < NewRelic::MetricParser
  def task
    segments[1]
  end
  
  def developer_name
    segments[2..-1].join(NewRelic::MetricParser::SEPARATOR)
  end
  
  def drilldown_url(metric_id)
    {:controller => '/v2/background_tasks', :action => 'index', :task => task, :anchor => "id=#{metric_id}"}
  end
end