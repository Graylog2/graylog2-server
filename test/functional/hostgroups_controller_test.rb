#require 'test_helper'
require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class HostgroupsControllerTest < ActionController::TestCase

  test "create a new host group" do
    assert_difference("Hostgroup.count") do
      post :create, :hostgroup => { :name => "some group" }
    end

    assert_redirected_to( :controller => "hosts", :action => "index" )
  end

  test "tabs are shown" do
    get(:show, :id => 3)

    assert_select("#content-tabs")
    assert_select(".content-tabs-tab", :minimum => 1)
  end

end
