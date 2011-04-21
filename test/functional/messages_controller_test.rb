require 'test_helper'

class MessagesControllerTest < ActionController::TestCase
  context "with some messages" do
    setup do
      @message = Message.make
    end

    context "GET index" do
      setup do
        get :index
      end

      should_respond_with :success
    end
  end
end
