# This is so that we don't detect a dispatcher like mongrel and think we are
# monitoring it.
ENV['NEWRELIC_DISPATCHER'] = 'none'

$LOAD_PATH << File.expand_path(File.join(File.dirname(__FILE__), '..','..'))

require 'new_relic/rack_app'

# Valid options which may be present in this binding:
# :license_key   optional license key override
# :app_name      optional name of app
# :logging       optional, false to omit request logging to stdout

options ||= {}

use Rack::CommonLogger unless options[:logging] == false
use Rack::ShowExceptions
map "/" do
  run NewRelic::Rack::Status.new
end
map "/metrics" do
  run NewRelic::Rack::MetricApp.new(options)
end


