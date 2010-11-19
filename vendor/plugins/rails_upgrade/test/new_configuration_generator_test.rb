require 'test_helper'
require 'new_configuration_generator'

# Stub out methods on upgrader class
module Rails
  module Upgrading
    class NewConfigurationGenerator
      attr_writer :environment_code
      
      def has_environment?
        true
      end
      
      def environment_code
        @environment_code
      end
      
      def app_name
        "my_application"
      end
    end
  end
end

class NewConfigurationGeneratorTest < ActiveSupport::TestCase
  FRAME = "# Put this in config/application.rb
require File.expand_path('../boot', __FILE__)

module MyApplication
  class Application < Rails::Application
%s
  end
end"

  CONFIG = "  config.what_have_you = 'thing'
  config.action_controller = 'what'"

  CODE = "require 'w/e'

this_happens_before_the(code)
more_before_the_code!

Rails::Initializer.run do |config|
%s
end

this_is_after_the_code
"

  def test_raises_error_with_no_code
    generator = Rails::Upgrading::NewConfigurationGenerator.new
    generator.environment_code = ""
    
    assert_raises(RuntimeError) { generator.generate_new_application_rb }
  end
  
  def test_generates_with_code
    generator = Rails::Upgrading::NewConfigurationGenerator.new
    generator.environment_code = CODE % [CONFIG]
    
    assert_equal FRAME % [generator.indent(CONFIG)], generator.generate_new_application_rb
  end
end