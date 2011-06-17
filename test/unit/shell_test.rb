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
      expected["_http_response_code"] = { :value => 500, :condition => "=" }
      expected["host"] =  { :value => "example.org", :condition => "=" }
      assert_equal expected, s.operator_options
    end

    should "not overwrite multiple operator options of the same type" do
      s = Shell.new('all.find(foo > 500, foo < 600, foo >= 700)') # i know, these conditions do not make sense
      expected = Hash.new
      expected["foo"] = Array.new
      expected["foo"] << { :value => 500, :condition => ">" }
      expected["foo"] << { :value => 600, :condition => "<" }
      expected["foo"] << { :value => 700, :condition => ">=" }
      assert_equal expected, s.operator_options
    end
    
    should "respect conditional operators" do
      3.times { Message.make(:host => "example.org", :_http_response_code => 500) }
      7.times { Message.make(:host => "example.com", :_http_response_code => 500) }
      8.times { Message.make(:host => "example.com", :_http_response_code => 200) }
      10.times { Message.make(:host => "example.com", :_http_response_code => 201) }
      1.times { Message.make(:host => "example.com", :_http_response_code => 300) }

      s = Shell.new('all.count(host != "example.org", _http_response_code < 300, _http_response_code >= 200)')
      result = s.compute
      
      assert_equal "count", result[:operation]
      assert_equal 18, result[:result]
    end

    should "work with different conditional operators on the same key" do
      5.times { Message.make(:_foo => 1) }
      2.times { Message.make(:_foo => 2) }
      2.times { Message.make(:_foo => 3) }
      2.times { Message.make(:_foo => 4) }

      s = Shell.new('all.count(_foo > 0, _foo != 3, _foo != 4)')
      result = s.compute
      
      assert_equal "count", result[:operation]
      assert_equal 7, result[:result]
    end

    should "work with regex operator options" do
      5.times { Message.make(:host => "example.org") }
      5.times { Message.make(:host => "example.com") }
      5.times { Message.make(:host => "foo.example.com") }

      s = Shell.new('all.count(host = /^example\.(org|com)$/)')
      result = s.compute

      assert_equal "count", result[:operation]
      assert_equal 10, result[:result]
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
