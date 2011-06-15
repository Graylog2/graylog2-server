require 'test_helper'

class ShellTest < ActiveSupport::TestCase

  context "command parsing" do

    should "parse selector" do
      s = Shell.new('all.find(_http_response_code => 500, host => "example.org")')
      assert_equal "all", s.selector
    end
    
    should "parse operator" do
      s = Shell.new('all.find(_http_response_code => 500, host => "example.org")')
      assert_equal "find", s.operator
    end
    
    should "parse operator options" do
      s = Shell.new('all.find(_http_response_code => 500, host => "example.org")')
      assert_equal [{"_http_response_code" => 500}, {"host" => "example.org"}], s.operator_options
    end

    should "throw exception for not allowed selector" do
      assert_raise InvalidSelectorException do
        Shell.new('nothing.find(_http_response_code => 500, host => "example.org")')
      end
    end
    
    should "throw exception for not allowed operator" do
      assert_raise InvalidOperatorException do
        Shell.new('all.something(_http_response_code => 500, host => "example.org")')
      end
    end
    
  end

end
