# This is the initialization for the RPM Rails plugin
require 'new_relic/control'

# If you are having problems seeing data, be sure and check the
# newrelic_agent log files. 
# 
# If you can't find any log files and you don't see anything in your
# application log files, try uncommenting the two lines at the 
# bottom of this file to verify the plugin is being loaded, 
# then send the output to support@newrelic.com if you are unable to 
# resolve the issue.

# Initializer for the NewRelic Agent

begin
  # JRuby's glassfish plugin is trying to run the Initializer twice,
  # which isn't a good thing so we ignore subsequent invocations here.
  if ! defined?(::NEWRELIC_STARTED)
    ::NEWRELIC_STARTED = "#{caller.join("\n")}"

    NewRelic::Control.instance.init_plugin(defined?(config) ? {:config => config} : {})
  else
    NewRelic::Control.instance.log.debug "Attempt to initialize the plugin twice!"
    NewRelic::Control.instance.log.debug "Original call: \n#{::NEWRELIC_STARTED}"
    NewRelic::Control.instance.log.debug "Here we are now: \n#{caller.join("\n")}"
  end
rescue => e
  NewRelic::Control.instance.log! "Error initializing New Relic plugin (#{e})", :error
  NewRelic::Control.instance.log!  e.backtrace.join("\n"), :error
  NewRelic::Control.instance.log! "Agent is disabled."
end
#ClassLoadingWatcher.flag_const_missing = nil

# ::RAILS_DEFAULT_LOGGER.warn "RPM detected environment: #{NewRelic::Control.instance.local_env.to_s}, RAILS_ENV: #{RAILS_ENV}"
# ::RAILS_DEFAULT_LOGGER.warn "Enabled? #{NewRelic::Control.instance.agent_enabled?}"
