require 'test_helper'

class HostsControllerTest < ActionController::TestCase

  should "destroy host" do
    host = Host.make
    
    assert_difference('Host.count', -1) do
      delete :destroy, :id => host.id.to_s
    end

    assert_redirected_to hosts_path
  end

  should "not delete messages with host" do
    host = Host.make
    15.times { bm(:host => host.host) }

    assert_equal 15, MessageGateway.total_count

    assert_difference('Host.count', -1) do
      delete :destroy, :id => host.id.to_s
    end

    # Should still be 15, man.
    assert_equal 15, MessageGateway.total_count
    
    assert_redirected_to hosts_path
  end

  should "not fail with non-existent host but redirect with error" do
    delete :destroy, :id => BSON::ObjectId.new

    assert_match /^Could not.+/, flash[:error]

    assert_redirected_to hosts_path
  end

  should "redirect correctly in quickjump" do
    host = Host.make
    post :quickjump, :host => host.host.to_param
    assert_redirected_to host_messages_path(host.host)
  end

end
