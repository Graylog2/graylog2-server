require 'test_helper'

class MessagesControllerTest < ActionController::TestCase
  context "message overview" do
    should "be shown" do
      10.times { bm }
      get :index
      assert_equal 10, assigns(:messages).count
      assert_select 'tr.message-row', 10
    end

    should "correctly paginate" do
      (Message::LIMIT+10).times { bm }

      get :index
      assert_equal Message::LIMIT, assigns(:messages).count

      get :index, :page => 2
      assert_equal 10, assigns(:messages).count
    end
  end

  context "messages of a stream" do

    should "correctly paginate" do
      stream = Stream.make
      (Message::LIMIT+10).times { bm(:streams => [stream.id]) }

      get :index, :stream_id => stream.id.to_s
      assert_equal stream.title, assigns(:stream).title # Make sure stream handling was detected.
      assert_equal Message::LIMIT, assigns(:messages).count

      get :index, :page => 2, :stream_id => stream.id.to_s
      assert_equal stream.title, assigns(:stream).title
      assert_equal 10, assigns(:messages).count
    end

  end

  context "messages of a host" do

    should "correctly paginate" do
      Host.make(:host => "somehost")
      (Message::LIMIT+10).times { bm(:host => "somehost") }

      get :index, :host_id => "somehost"
      assert_equal "somehost", assigns(:host).host # Make sure host handling was detected.
      assert_equal Message::LIMIT, assigns(:messages).count

      get :index, :page => 2, :host_id => "somehost"
      assert_equal "somehost", assigns(:host).host
      assert_equal 10, assigns(:messages).count
    end

  end

  context "deleting" do

    should "delete a single message" do
      message = bm()
      id = message["_id"]
      delete :destroy, :id => id

      assert_nil flash[:error]
      assert_redirected_to messages_path
    end

    should "complain about non existent message" do
      delete :destroy, :id => "lulz-idontexist"

      assert_not_nil flash[:error]
      assert_redirected_to messages_path
    end

  end

end
