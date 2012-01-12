require 'test_helper'

class MessageGatewayTest < ActiveSupport::TestCase

  context "deleting" do

    should "delete a message" do
      message = bm()
      id = message["_id"]

      assert MessageGateway.delete_message(id)
    end

    should "complain about non existent message" do
      assert !MessageGateway.delete_message("LOLWUT-DONTEXIST")
    end

  end

  context "analyzing" do
    
    should "correctly analyze a text" do
      # !! this will FAIL if a wrong analyzer was provided in mapping
      assert_equal ["LOLWUT", "zomg.wat,", "ohai"], MessageGateway.analyze("LOLWUT zomg.wat, ohai", "message")
    end

    should "not fail on empty text" do
      assert_equal Array.new, MessageGateway.analyze("", "message")
    end

  end

end
