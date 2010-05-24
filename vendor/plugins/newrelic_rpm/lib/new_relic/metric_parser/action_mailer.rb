class NewRelic::MetricParser::ActionMailer < NewRelic::MetricParser
  
  def is_action_mailer?; true; end
    
  def short_name
    "ActionMailer - #{segments[1]}"
  end
  
end