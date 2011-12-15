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
end
