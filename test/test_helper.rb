ENV["RAILS_ENV"] = "test"
require File.expand_path('../../config/environment', __FILE__)
require 'rails/test_help'

require File.expand_path(File.dirname(__FILE__) + "/blueprints-mm.rb")
require File.expand_path(File.dirname(__FILE__) + "/blueprints-ar.rb")

class ActiveSupport::TestCase
  # Setup all fixtures in test/fixtures/*.(yml|csv) for all tests in alphabetical order.
  #
  # Note: You'll currently still have to declare fixtures explicitly in integration tests
  # -- they do not yet inherit this setting
  fixtures :all

  setup { Sham.reset }

  # Reset MongoDB test databases.
  Host.delete_all
  Message.delete_all

  # Log in user.
  def setup
    # Only for functionals.
    unless @request == nil or @request.cookies == nil
      @request.cookies["auth_token"] = cookie_for(:quentin)
    end
  end
  
  protected
    def auth_token(token)
      CGI::Cookie.new('name' => 'auth_token', 'value' => token)
    end
    
    def cookie_for(user)
      auth_token users(user).remember_token
    end

end
