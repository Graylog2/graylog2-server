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
