require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class AnalyticsControllerTest < ActionController::TestCase

  # Most stuff tested in shell unit tests
  context "shell" do

    should "count" do
      10.times { bm(:host => "example.org") }
      15.times { bm(:host => "example.com") }

      query = 'all.count(host = "example.com")'
      post :shell, :cmd => query
      
      result = assigns(:result)
      assert_equal "count", result[:operation]
      assert_equal 15, result[:result]
    end

    should "find" do
      10.times { bm(:host => "example.org") }
      msg1 = bm(:host => "example.com")
      msg2 = bm(:host => "example.com")

      query = 'all.find(host = "example.com")'
      post :shell, :cmd => query

      result = assigns(:result)
      assert_equal "find", result[:operation]
      assert_equal 2, result[:result].count
    end

    should "distinct" do
      5.times { bm(:host => "foo.example.org", :_something => "lolwat") }
      2.times { bm(:host => "foo.example.org", :_something => "zomg" ) }
      4.times { bm(:host => "bar.example.com", :_something => "nothing here") }

      query = 'all.distribution({_something}, host = "foo.example.org")'
      post :shell, :cmd => query

      result = assigns(:result)
      assert_equal "distribution", result[:operation]
      assert_equal [{"distinct"=>"lolwat", "count"=>5}, {"distinct"=>"zomg", "count"=>2}], result[:result]
    end

  end

end
