require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class AnalyticsControllerTest < ActionController::TestCase

  # Most stuff tested in shell unit tests
  context "shell" do

    should "count" do
      10.times { Message.make(:host => "example.org") }
      15.times { Message.make(:host => "example.com") }

      query = 'all.count(host = "example.com")'
      post :shell, :cmd => query

      result = assigns(:result)
      assert_equal "count", result[:operation]
      assert_equal 15, result[:result]
    end

    should "find" do
      10.times { Message.make(:host => "example.org") }
      msg1 = Message.make(:host => "example.com")
      msg2 = Message.make(:host => "example.com")

      query = 'all.find(host = "example.com")'
      post :shell, :cmd => query

      result = assigns(:result)
      assert_equal "find", result[:operation]
      assert_equal [msg1, msg2], result[:result]
    end

    should "distinct" do
      5.times { Message.make(:host => "foo.example.org") }
      4.times { Message.make(:host => "bar.example.com") }
      2.times { Message.make(:host => "baz.example.org") }

      query = 'all.distinct({host}, host = /^(foo|baz)/)'
      post :shell, :cmd => query

      result = assigns(:result)
      assert_equal "distinct", result[:operation]
      assert_equal ["foo.example.org", "baz.example.org"], result[:result]
    end

    should "return a general error if the mongo operation failed" do
      Shell.any_instance.expects(:perform_find).raises(Mongo::OperationFailure)

      query = 'all.find(foo = "bar")'
      post :shell, :cmd => query

      r = JSON.parse(@response.body)

      assert_equal "error", r["code"]
      assert_match /^Mongo operation failed/, r["reason"]
    end

    should "return a descriptive error if the mongo operation was interrupted" do
      Shell.any_instance.expects(:perform_find).raises(Mongo::OperationFailure, "interrupted")

      query = 'all.find(foo = "bar")'
      post :shell, :cmd => query

      r = JSON.parse(@response.body)

      assert_equal "error", r["code"]
      assert_match /^Mongo operation was interrupted/, r["reason"]
    end

  end

end
