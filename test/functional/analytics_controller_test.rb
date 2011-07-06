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

  end

end
