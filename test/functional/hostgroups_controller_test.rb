require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class HostgroupsControllerTest < ActionController::TestCase

  test "create a new host group" do
    assert_difference("Hostgroup.count") do
      post :create, :hostgroup => { :name => "some group" }
    end

    assert_redirected_to(:controller => "hosts", :action => "index")
  end

  test "delete a host group" do
    assert_difference("Hostgroup.count", -1) do
      post :destroy, :id => 1
    end

    assert_redirected_to(:controller => "hosts", :action => "index");
  end

  test "hosts are deleted with hostgroup" do
      post :destroy, :id => 1
      assert(HostgroupHost.count(:conditions => ["hostgroup_id = 1"]) == 0, "Hosts were not deleted.")
  end

  test "rename a host group" do
    post :rename, :name => "hey my new name", :id => 1

    renamed_group = Hostgroup.find(1)
    assert renamed_group.name == "hey my new name"

    assert_redirected_to(:controller => "hostgroups", :action => "settings", :id => 1)
  end

  test "renaming a host group does not work if no name is given" do
    old_group = Hostgroup.find(1)
    post :rename, :name => "", :id => 1

    renamed_group = Hostgroup.find(1)

    # Name did not change.
    assert renamed_group.name == old_group.name, "Oh no, group was renamed!"

    # Error is set.
    assert_not_nil flash[:error]

    assert_redirected_to(:controller => "hostgroups", :action => "settings", :id => 1)
  end

  test "tabs are shown" do
    get :show, :id => 3

    assert_select("#content-tabs")
    assert_select(".content-tabs-tab", :minimum => 1)
  end

end
