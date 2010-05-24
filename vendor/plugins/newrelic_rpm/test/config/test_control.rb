require 'new_relic/control/rails'

class NewRelic::Control::Test < NewRelic::Control::Rails #:nodoc:
  def env
    'test'
  end
  def app
    :rails
  end
  def config_file
    File.join(File.dirname(__FILE__), "newrelic.yml")
  end
  def initialize local_env
    super local_env
    setup_log 
  end
  # when running tests, don't write out stderr
  def log!(msg, level=:info)
    log.send level, msg if log
  end

  # Add the default route in case it's missing.  Need it for testing.
  def install_devmode_route
    super
    ActionController::Routing::RouteSet.class_eval do
      return if defined? draw_without_test_route
      def draw_with_test_route
        draw_without_test_route do | map |
          map.connect ':controller/:action/:id'
          yield map        
        end
      end
      alias_method_chain :draw, :test_route
    end
    # Force the routes to be reloaded
    ActionController::Routing::Routes.reload!
  end
end