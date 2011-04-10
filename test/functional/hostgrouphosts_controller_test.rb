require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class HostgroupHostsControllerTest < ActionController::TestCase

  test "add host with type simple" do
    Host.make(:host => "foo").save

    hostgroup_id = 100
    Hostgroup.make(:id => hostgroup_id).save

    assert_difference("HostgroupHost.count") do
      post :create, :new_host => { :hostname => "foo", :hostgroup_id => hostgroup_id, :ruletype => HostgroupHost::TYPE_SIMPLE }
    end

    # Test that the host really has TYPE_SIMPLE (and hostname if we are already here...)
    hosts = HostgroupHost.find_all_by_hostgroup_id(hostgroup_id)
    assert_equal 1, hosts.count
    assert_equal HostgroupHost::TYPE_SIMPLE, hosts[0].ruletype
    assert_equal "foo", hosts[0].hostname

    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => hostgroup_id)
  end

  test "add host with type regex" do
    hostgroup_id = 150
    Hostgroup.make(:id => hostgroup_id).save

    assert_difference("HostgroupHost.count") do
      post :create, :new_host => { :hostname => "^foo\d", :hostgroup_id => hostgroup_id, :ruletype => HostgroupHost::TYPE_REGEX }
    end

    # Test that the host really has TYPE_REGEX (and hostname if we are already here...)
    hosts = HostgroupHost.find_all_by_hostgroup_id(hostgroup_id)
    assert_equal 1, hosts.count
    assert_equal HostgroupHost::TYPE_REGEX, hosts[0].ruletype
    assert_equal "^foo\d", hosts[0].hostname

    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => hostgroup_id)
  end

  test "host group must exist" do
    Host.make(:host => "foo").save
    post :create, :new_host => { :hostname => "foo", :hostgroup_id => 13371337, :ruletype => HostgroupHost::TYPE_SIMPLE }

    assert_equal "Group does not exist.", flash[:error]

    assert_redirected_to(:controller => "hosts", :action => "index")
  end

  test "simple host is not added if host does not exist" do
    hostgroup_id = 500
    Hostgroup.make(:id => hostgroup_id).save

    post :create, :new_host => { :hostname => "i_dont_exist", :hostgroup_id => hostgroup_id, :ruletype => HostgroupHost::TYPE_SIMPLE }

    assert_equal "Host does not exist!", flash[:error]

    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => hostgroup_id)
  end

  test "a host can only be added once to a host group" do
    Host.make(:host => "foo").save

    hostgroup_id = 50
    Hostgroup.make(:id => hostgroup_id).save

    # First insert should work.
    assert_difference("HostgroupHost.count") do
      post :create, :new_host => { :hostname => "foo", :hostgroup_id => hostgroup_id, :ruletype => HostgroupHost::TYPE_SIMPLE }
    end

    # First redirect
    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => hostgroup_id)

    # Insert host a second time.
    assert_no_difference("HostgroupHost.count") do
      post :create, :new_host => { :hostname => "foo", :hostgroup_id => hostgroup_id, :ruletype => HostgroupHost::TYPE_SIMPLE }
    end

    assert_equal "Host already in group.", flash[:error]

    # Second redirect
    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => hostgroup_id)
  end

end
