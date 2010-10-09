# Bootstrap the Rails environment, frameworks, and default configuration
require File.join(File.dirname(__FILE__), 'boot')

require 'mongo_mapper'

Rails::Initializer.run do |config|
  # nothing
end
