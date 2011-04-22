require 'test_helper'

class MessagesControllerTest < ActionController::TestCase
  context "message overview" do
    setup do
      @message = Message.make
    end

    context "GET index" do
      setup do
        get :index
      end

      should_respond_with :success
    end

    should "correctly paginate" do
      (Message::LIMIT+10).times { Message.make }

      get :index
      assert_equal Message::LIMIT, assigns(:messages).count

      get :index, :page => 2
      assert_equal 10, assigns(:messages).count
    end

  end

  context "messages of a stream" do

    should "correctly paginate" do
      stream = Stream.make
      (Message::LIMIT+10).times { Message.make(:streams => [stream.id]) }

      get :index, :stream_id => stream.id.to_s
      assert_equal stream.title, assigns(:stream).title # Make sure stream handling was detected.
      assert_equal Message::LIMIT, assigns(:messages).count

      get :index, :page => 2, :stream_id => stream.id.to_s
      assert_equal stream.title, assigns(:stream).title
      assert_equal 10, assigns(:messages).count
    end

>>>>>>> hotfix/broken-host-pagination-37
  end

  context "messages of a host" do

    should "correctly paginate" do
      Host.make(:host => "somehost")
      (Message::LIMIT+10).times { Message.make(:host => "somehost") }

      get :index, :host_id => "somehost"
      assert_equal "somehost", assigns(:host).host # Make sure host handling was detected.
      assert_equal Message::LIMIT, assigns(:messages).count

      get :index, :page => 2, :host_id => "somehost"
      assert_equal "somehost", assigns(:host).host
      assert_equal 10, assigns(:messages).count
    end

  end

end
