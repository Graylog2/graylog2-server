require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class HostgroupTest < ActiveSupport::TestCase

  test "hostname_conditions with ids in returned hash" do
    # Create the hosts that are defined as hostnames in the group
    # because hostname_conditions() will check if the hosts exist.
    Host.make(:host => "host1")
    Host.make(:host => "host2")

    group = Hostgroup.make
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => "host1", :ruletype => HostgroupHost::TYPE_SIMPLE)
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => "host2", :ruletype => HostgroupHost::TYPE_SIMPLE)
    
    conditions = group.hostname_conditions(true)
    
    assert_instance_of Array, conditions
    assert conditions.count == 2

    conditions.each do |condition|
      assert_instance_of Hash, condition, "Condition is not a hash"
      assert_kind_of BSON::ObjectId, condition[:id]
      assert_instance_of String, condition[:value]
      assert condition[:value].length > 0
    end
  end

  test "hostname_conditions without ids in returned hash" do
    # Create the hosts that are defined as hostnames in the group
    # because hostname_conditions() will check if the hosts exist.
    Host.make(:host => "host1")
    Host.make(:host => "host2")

    group = Hostgroup.make
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => "host1", :ruletype => HostgroupHost::TYPE_SIMPLE)
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => "host2", :ruletype => HostgroupHost::TYPE_SIMPLE)
    
    conditions = group.hostname_conditions

    assert_instance_of Array, conditions
    assert conditions.count == 2

    conditions.each do |condition|
      assert_instance_of String, condition, "Condition is not a string/hostname"
      assert condition.length > 0
    end
  end

  test "regex_conditions with ids in returned hash" do
    group = Hostgroup.make

    HostgroupHost.make(:hostgroup_id => group.id, :hostname => /^foo/, :ruletype => HostgroupHost::TYPE_REGEX)
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => /bar.+baz/, :ruletype => HostgroupHost::TYPE_REGEX)
    
    conditions = group.regex_conditions(true)

    assert_instance_of Array, conditions
    assert conditions.count == 2

    conditions.each do |condition|
      assert_instance_of Hash, condition, "Condition is not a hash"
      assert_kind_of BSON::ObjectId, condition[:id]
      assert_instance_of Regexp, condition[:value]
    end
  end

  test "regex_conditions without ids in returned hash" do
    group = Hostgroup.make

    HostgroupHost.make(:hostgroup_id => group.id, :hostname => /^foo/, :ruletype => HostgroupHost::TYPE_REGEX)
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => /bar.+baz/, :ruletype => HostgroupHost::TYPE_REGEX)
    conditions = group.regex_conditions

    assert_instance_of Array, conditions
    assert conditions.count == 2

    conditions.each do |condition|
      assert_instance_of Regexp, condition, "Condition is not a regular expression"
    end
  end

  test "all_conditions" do
    # Create the host that is defined as hostname in the group
    # because all_conditions() will check if the host exists.
    Host.make(:host => "somehost")
    
    group = Hostgroup.make
    
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => "somehost", :ruletype => HostgroupHost::TYPE_SIMPLE)
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => /bar.+baz/, :ruletype => HostgroupHost::TYPE_REGEX)
    HostgroupHost.make(:hostgroup_id => group.id, :hostname => /foo.+baz/, :ruletype => HostgroupHost::TYPE_REGEX)
    conditions = group.all_conditions

    assert_instance_of Array, conditions
    assert_equal 3, conditions.count

    # Check regex types.
    assert_instance_of Regexp, conditions[2]
    assert_instance_of Regexp, conditions[1]

    # Check simple type.
    assert_instance_of String, conditions[0]
    assert conditions[0].length > 0
  end

end

