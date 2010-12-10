ENV["RAILS_ENV"] = "test"
require File.expand_path('../../config/environment', __FILE__)
require 'rails/test_help'

require File.expand_path(File.dirname(__FILE__) + "/blueprints-mm.rb")
require File.expand_path(File.dirname(__FILE__) + "/blueprints-ar.rb")

DatabaseCleaner[:mongo_mapper].strategy = :truncation


class ActiveSupport::TestCase
  fixtures :all
  self.use_instantiated_fixtures = false
  self.use_transactional_fixtures = true

  setup do
    DatabaseCleaner.clean
    Sham.reset
  end
end


class ActionController::TestCase
  setup do
    @request.cookies["auth_token"] = cookie_for(:quentin)
  end

protected
  def auth_token(token)
    CGI::Cookie.new('name' => 'auth_token', 'value' => token)
  end

  def cookie_for(user)
    auth_token users(user).remember_token
  end
end
