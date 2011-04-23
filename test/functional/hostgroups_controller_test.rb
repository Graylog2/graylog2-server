require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class HostgroupsControllerTest < ActionController::TestCase

  test "create a new host group" do
    assert_difference("Hostgroup.count") do
      post :create, :hostgroup => { :name => "some group" }
    end

    assert_redirected_to(:controller => "hosts", :action => "index")
  end

  test "delete a host group" do
    group = Hostgroup.make
    assert_difference("Hostgroup.count", -1) do
      post :destroy, :id => group.id.to_s
    end

    assert_redirected_to(:controller => "hosts", :action => "index");
  end

  test "hosts are deleted with hostgroup" do
    group = Hostgroup.make

    HostgroupHost.make(:hostgroup_id => group.id, :hostname => "somehost", :ruletype => HostgroupHost::TYPE_SIMPLE)
    post :destroy, :id => group.id.to_s

    assert_equal 0, HostgroupHost.where(:hostgroup_id => group.id).count
  end

  test "rename a host group" do
    group = Hostgroup.make(:name => "some group")

    post :rename, :name => "hey my new name", :id => group.id.to_s
    renamed_group = Hostgroup.find(group.id)

    assert_equal "hey my new name", renamed_group.name
    assert_redirected_to(:controller => "hostgroups", :action => "settings", :id => group.id.to_s)
  end

  test "renaming a host group does not work if no name is given" do
    group = Hostgroup.make
    post :rename, :name => "", :id => group.id.to_s

    possibly_renamed_group = Hostgroup.find(group.id)

    # Name did not change.
    assert_equal possibly_renamed_group.name, group.name

    # Error is set.
    assert_not_nil flash[:error]

    assert_redirected_to(:controller => "hostgroups", :action => "settings", :id => group.id)
  end

  test "tabs are shown" do
    group = Hostgroup.make
    get :show, :id => group.id.to_s

    assert_select("#content-tabs")
    assert_select(".content-tabs-tab", :minimum => 1)
  end

end
