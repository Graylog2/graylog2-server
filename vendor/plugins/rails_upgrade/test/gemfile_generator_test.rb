require 'test_helper'
require 'gemfile_generator'

# Stub out methods on upgrader class
module Rails
  module Upgrading
    class GemfileGenerator
      attr_writer :environment_code
      
      def has_environment?
        true
      end
      
      def environment_code
        @environment_code
      end
    end
  end
end

class GemfileGeneratorTest < ActiveSupport::TestCase
  PREAMBLE = <<STR
# Edit this Gemfile to bundle your application's dependencies.
# This preamble is the current preamble for Rails 3 apps; edit as needed.
source 'http://rubygems.org'

gem 'rails', '3.0.6'

STR

  def test_generates_with_no_gems
    generator = Rails::Upgrading::GemfileGenerator.new
    generator.environment_code = ""
    
    assert_equal PREAMBLE, generator.generate_gemfile
  end
  
  def test_generates_with_gem
    generator = Rails::Upgrading::GemfileGenerator.new
    generator.environment_code = "config.gem 'camping'"
    
    assert_equal PREAMBLE + "gem 'camping'", generator.generate_gemfile
  end
  
  def test_generates_with_version
    generator = Rails::Upgrading::GemfileGenerator.new
    generator.environment_code = "config.gem 'camping', :version => '2.1.1'"
    
    assert_equal PREAMBLE + "gem 'camping', '2.1.1'", generator.generate_gemfile
  end
  
  def test_can_add_sources
    generator = Rails::Upgrading::GemfileGenerator.new
    generator.environment_code = "config.gem 'camping', :source => 'http://code.whytheluckystiff.net'"
    
    assert_equal PREAMBLE + "source 'http://code.whytheluckystiff.net'\ngem 'camping'", generator.generate_gemfile
  end
  
  def test_changes_lib_to_new_key
    generator = Rails::Upgrading::GemfileGenerator.new
    generator.environment_code = "config.gem 'camping', :lib => 'kamping'"
    
    assert_equal PREAMBLE + "gem 'camping', :require => 'kamping'", generator.generate_gemfile    
  end
  
  def test_generates_with_all_options
    generator = Rails::Upgrading::GemfileGenerator.new
    generator.environment_code = "config.gem 'camping', :lib => 'kamping', :source => 'http://code.whytheluckystiff.net', :version => '2.1.1'"
    
    assert_equal PREAMBLE + "source 'http://code.whytheluckystiff.net'\ngem 'camping', '2.1.1', :require => 'kamping'", generator.generate_gemfile     
  end
end
