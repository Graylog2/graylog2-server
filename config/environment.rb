# Bootstrap the Rails environment, frameworks, and default configuration
require File.join(File.dirname(__FILE__), 'boot')

require 'mongo_mapper'

Rails::Initializer.run do |config|
  config.gem 'json', :version => '1.4.3'
  config.gem 'bson', :version => '1.0.1'
  config.gem 'mongo', :version => '1.0.1'
  config.gem 'bson_ext', :version => '1.0.1'
  config.gem 'mongo_mapper', :version => '0.8.2'
  config.gem 'jnunemaker-validatable', :version => '1.8.4' # Dependency of mongo_mapper
  config.gem 'plucky', :version => '0.3.2' # Dependency of mongo_mapper
  config.gem 'mysql', :version => '2.8.1'
  config.gem 'rack', :version => '1.1.0'

  config.time_zone = 'Berlin'
end
