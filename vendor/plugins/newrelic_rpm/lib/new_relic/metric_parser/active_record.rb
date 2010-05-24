class NewRelic::MetricParser::ActiveRecord < NewRelic::MetricParser
  def is_active_record? ; true; end
  
  def model_class
    return segments[1]
  end
  
  def legend_name
    if name == 'ActiveRecord/all'
      'Database'
    else
      super
    end
  end
  def tooltip_name
    if name == 'ActiveRecord/all'
      'all SQL execution'
    else
      super
    end
  end
  def developer_name
    "#{model_class}##{segments.last}"
  end
end