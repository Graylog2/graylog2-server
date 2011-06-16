require 'test_helper'

class ShellTest < ActiveSupport::TestCase

  context "command parsing" do

    should "parse selector" do
      s = Shell.new('all.find(_http_response_code = 500, host = "example.org")')
      assert_equal "all", s.selector
    end
    
    should "parse operator" do
      s = Shell.new('all.find(_http_response_code = 500, host = "example.org")')
      assert_equal "find", s.operator
    end
    
    should "parse operator options" do
      s = Shell.new('all.find(_http_response_code = 500, host = "example.org")')
      expected = Hash.new
      expected["_http_response_code"] = 500
      expected["host"] = "example.org"
      assert_equal expected, s.operator_options
    end

    should "respect conditional operators" do
      #3.times { Message.make(:host => "example.org", :_http_response_code => 500) }
      #7.times { Message.make(:host => "example.com", :_http_response_code => 500) }
      #8.times { Message.make(:host => "example.org", :_http_response_code => 200) }

      #s = Shell.new('all.count(host !=> "example.org", _http_response_code => 500)')
      #result = s.compute
      
      #assert_equal "count", result[:operation]
      #assert_equal 3, result[:result]
    end

    should "throw exception for not allowed selector" do
      assert_raise InvalidSelectorException do
        Shell.new('nothing.find(_http_response_code = 500, host = "example.org")')
      end
    end
    
    should "throw exception for not allowed operator" do
      assert_raise InvalidOperatorException do
        Shell.new('all.something(_http_response_code = 500, host = "example.org")')
      end
    end
    
  end

  context "counting" do

    should "count all with no options" do
      17.times { Message.make }
      s = Shell.new("all.count()")
      result = s.compute
      
      assert_equal "count", result[:operation]
      assert_equal 17, result[:result]
    end

    should "count all with options" do
      10.times { Message.make(:host => "example.org") }
      15.times { Message.make(:host => "example.com") }

      s = Shell.new('all.count(host = "example.com")')
      result = s.compute
      
      assert_equal "count", result[:operation]
      assert_equal 15, result[:result]
    end
    
    should "count all with options including integer option" do
      3.times { Message.make(:host => "example.org", :_http_response_code => 500) }
      7.times { Message.make(:host => "example.com", :_http_response_code => 500) }
      8.times { Message.make(:host => "example.org", :_http_response_code => 200) }

      s = Shell.new('all.count(host = "example.org", _http_response_code = 500)')
      result = s.compute
      
      assert_equal "count", result[:operation]
      assert_equal 3, result[:result]
    end

  end

end
