ENV["RAILS_ENV"] = "test"
require File.expand_path('../../config/environment', __FILE__)
require 'rails/test_help'

require 'blueprints'

class ActiveSupport::TestCase
  setup do
    DatabaseCleaner.clean
    Sham.reset
  end
end
