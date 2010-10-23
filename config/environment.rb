# Bootstrap the Rails environment, frameworks, and default configuration
require File.join(File.dirname(__FILE__), 'boot')

require 'mongo_mapper'

Rails::Initializer.run do |config|
  config.gem 'json', :version => '1.4.6'
  config.gem 'bson', :version => '1.1.1'
  config.gem 'mongo', :version => '1.1.1'
  config.gem 'bson_ext', :version => '1.1.1', :lib => false
  config.gem 'mongo_mapper', :version => '0.8.6'
  config.gem 'jnunemaker-validatable', :version => '1.8.4', :lib => false # Dependency of mongo_mapper
  config.gem 'plucky', :version => '0.3.6' # Dependency of mongo_mapper
  config.gem 'mysql', :version => '2.8.1'
  config.gem 'rack', :version => '1.1.0'

  config.time_zone = 'Berlin'
end
