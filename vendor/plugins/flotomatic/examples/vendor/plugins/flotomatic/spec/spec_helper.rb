ENV["RAILS_ENV"] = "test"
require File.join(File.dirname(__FILE__), '..', '..', '..', '..', 'config', 'environment')
require 'spec'
require 'spec/rails'
require 'activesupport'
require 'actionpack'

require File.join(File.dirname(__FILE__), '..', 'lib', 'flot')
require File.join(File.dirname(__FILE__), '..', 'lib', 'time_flot')

$LOAD_PATH << File.join(File.dirname(__FILE__), '..', 'app', 'helpers')
ActiveSupport::Dependencies.load_paths << File.join(File.dirname(__FILE__), '..', 'app', 'helpers')

Spec::Runner.configure do |config|
  # If you're not using ActiveRecord you should remove these
  # lines, delete config/database.yml and disable :active_record
  # in your config/boot.rb
  config.use_transactional_fixtures = true
  config.use_instantiated_fixtures  = false
end
