ENV["RAILS_ENV"] = "test"
require File.expand_path('../../config/environment', __FILE__)
require 'rails/test_help'

require File.expand_path(File.dirname(__FILE__) + "/blueprints.rb")

class ActiveSupport::TestCase
  setup do
    DatabaseCleaner.clean
    Sham.reset
  end
end


class ActionController::TestCase
  setup do
    login!
  end

  def login!(options = {})
    user = User.make(options)
    @request.session[:user_id] = user.id
    user
  end
end
