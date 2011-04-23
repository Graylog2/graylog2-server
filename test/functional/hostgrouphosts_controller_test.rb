require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class HostgroupHostsControllerTest < ActionController::TestCase

  test "add host with type simple" do
    Host.make(:host => "foo").save

    group = Hostgroup.make

    assert_difference("HostgroupHost.count") do
      post :create, :new_host => { :hostname => "foo", :hostgroup_id => group.id.to_s, :ruletype => HostgroupHost::TYPE_SIMPLE }
    end

    # Test that the host really has TYPE_SIMPLE (and hostname if we are already here...)
    hosts = HostgroupHost.where(:hostgroup_id => group.id).all
    assert_equal 1, hosts.count
    assert_equal HostgroupHost::TYPE_SIMPLE, hosts[0].ruletype
    assert_equal "foo", hosts[0].hostname

    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => group.id)
  end

  test "add host with type regex" do
    group = Hostgroup.make

    assert_difference("HostgroupHost.count") do
      post :create, :new_host => { :hostname => "^foo\d", :hostgroup_id => group.id.to_s, :ruletype => HostgroupHost::TYPE_REGEX }
    end

    # Test that the host really has TYPE_REGEX (and hostname if we are already here...)
    hosts = HostgroupHost.where(:hostgroup_id => group.id).all
    assert_equal 1, hosts.count
    assert_equal HostgroupHost::TYPE_REGEX, hosts[0].ruletype
    assert_equal "^foo\d", hosts[0].hostname

    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => group.id)
  end

  test "host group must exist" do
    Host.make(:host => "foo")
    post :create, :new_host => { :hostname => "foo", :hostgroup_id => BSON::ObjectId.new.to_s, :ruletype => HostgroupHost::TYPE_SIMPLE }

    assert_equal "Group does not exist.", flash[:error]

    assert_redirected_to(:controller => "hosts", :action => "index")
  end

  test "simple host is not added if host does not exist" do
    group = Hostgroup.make

    post :create, :new_host => { :hostname => "i_dont_exist", :hostgroup_id => group.id.to_s, :ruletype => HostgroupHost::TYPE_SIMPLE }

    assert_equal "Host does not exist!", flash[:error]

    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => group.id)
  end

  test "a host can only be added once to a host group" do
    Host.make(:host => "foo")

    group = Hostgroup.make

    # First insert should work.
    assert_difference("HostgroupHost.count") do
      post :create, :new_host => { :hostname => "foo", :hostgroup_id => group.id.to_s, :ruletype => HostgroupHost::TYPE_SIMPLE }
    end

    # First redirect
    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => group.id)

    # Insert host a second time.
    assert_no_difference("HostgroupHost.count") do
      post :create, :new_host => { :hostname => "foo", :hostgroup_id => group.id.to_s, :ruletype => HostgroupHost::TYPE_SIMPLE }
    end

    assert_equal "Host already in group.", flash[:error]

    # Second redirect
    assert_redirected_to(:controller => "hostgroups", :action => "hosts", :id => group.id)
  end

end
