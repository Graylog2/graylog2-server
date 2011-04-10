require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class HostgroupTest < ActiveSupport::TestCase

  test "hostname_conditions with ids in returned hash" do
    # Create the hosts that are defined as hostnames in the group
    # because hostname_conditions() will check if the hosts exist.
    Host.make(:host => "host1").save
    Host.make(:host => "host2").save

    group = Hostgroup.find(1)
    conditions = group.hostname_conditions(true)

    assert_instance_of Array, conditions
    assert conditions.count == 2

    conditions.each do |condition|
      assert_instance_of Hash, condition, "Condition is not a hash"
      assert_kind_of Integer, condition[:id]
      assert_instance_of String, condition[:value]
      assert condition[:value].length > 0
    end
  end

  test "hostname_conditions without ids in returned hash" do
    # Create the hosts that are defined as hostnames in the group
    # because hostname_conditions() will check if the hosts exist.
    Host.make(:host => "host1").save
    Host.make(:host => "host2").save

    group = Hostgroup.find(1)
    conditions = group.hostname_conditions

    assert_instance_of Array, conditions
    assert conditions.count == 2

    conditions.each do |condition|
      assert_instance_of String, condition, "Condition is not a string/hostname"
      assert condition.length > 0
    end
  end

  test "regex_conditions with ids in returned hash" do
    group = Hostgroup.find(2)
    conditions = group.regex_conditions(true)

    assert_instance_of Array, conditions
    assert conditions.count == 2

    conditions.each do |condition|
      assert_instance_of Hash, condition, "Condition is not a hash"
      assert_kind_of Integer, condition[:id]
      assert_instance_of Regexp, condition[:value]
    end
  end

  test "regex_conditions without ids in returned hash" do
    group = Hostgroup.find(2)
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
    Host.make(:host => "somehost").save
    group = Hostgroup.find(3)
    conditions = group.all_conditions

    assert_instance_of Array, conditions
    assert conditions.count == 3

    # Check regex types.
    assert_instance_of Regexp, conditions[2]
    assert_instance_of Regexp, conditions[1]

    # Check simple type.
    assert_instance_of String, conditions[0]
    assert conditions[0].length > 0
  end

  test "treat host with no type as hostname" do
    Host.make(:host => "host1").save

    group = Hostgroup.find(4)
    conditions = group.hostname_conditions

    assert_instance_of Array, conditions
    assert_equal 1, conditions.count

    assert_equal "host1", conditions[0]
  end
end

