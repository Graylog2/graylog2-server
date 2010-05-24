require 'rack/response'
require 'newrelic_rpm'

# Rack middlewares and applications used by New Relic
require 'new_relic/rack/metric_app'