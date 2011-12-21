require File.expand_path(File.dirname(__FILE__) + "/../../test_helper")

class StreamsHelperTest < ActionView::TestCase

  include ApplicationHelper

  context "streamrule_to_human" do

    should "return improved human readable format for severity" do
      rule = Streamrule.make(:rule_type => Streamrule::TYPE_SEVERITY, :value => "1")
      assert_match /Severity.+:.+Alert \(1\)/, streamrule_to_human(rule)
    end

  end

end
