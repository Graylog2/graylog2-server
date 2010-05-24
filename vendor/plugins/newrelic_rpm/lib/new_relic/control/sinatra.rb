
require 'new_relic/control/ruby'

class NewRelic::Control::Sinatra < NewRelic::Control::Ruby
  
  def env
    @env ||= ENV['RACK_ENV'] || ENV['RAILS_ENV'] || 'development'
  end

  # This is the control used when starting up in the context of 
  # The New Relic Infrastructure Agent.  We want to call this
  # out specifically because in this context we are not monitoring
  # the running process, but actually external things.
  def init_config(options={})
    super
  end
  
end