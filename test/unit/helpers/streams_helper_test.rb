require File.expand_path(File.dirname(__FILE__) + "/../../test_helper")

class StreamsHelperTest < ActionView::TestCase

  include ApplicationHelper

  context "streamrule_to_human" do

    should "return improved human readable format for severity" do
      rule = Streamrule.make(:rule_type => Streamrule::TYPE_SEVERITY, :value => "1")
      assert_match /Severity.+:.+Alert \(1\)/, streamrule_to_human(rule)
    end

    should "return name of hostgroup" do
      group = Hostgroup.make(:name => "foo")
      rule = Streamrule.make(:rule_type => Streamrule::TYPE_HOSTGROUP, :value => group.id.to_s)
      assert_match /Hostgroup.+:.+foo/, streamrule_to_human(rule)
    end

    should "ignore but show not existing hostgroups" do
      random_id = BSON::ObjectId.new
      rule = Streamrule.make(:rule_type => Streamrule::TYPE_HOSTGROUP, :value => random_id)
      assert_match /Hostgroup.+:.+Unknown \(#{random_id.to_s}\)/, streamrule_to_human(rule)
    end

  end

end
