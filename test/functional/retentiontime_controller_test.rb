require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class RetentiontimeControllerTest < ActionController::TestCase

  should "provide correct variables for index action" do
    Setting.make(:user_id => @logged_in_user.id, :setting_type => Setting::TYPE_RETENTION_TIME_DAYS, :value => 100) 
    Setting.make(:user_id => @logged_in_user.id, :setting_type => Setting::TYPE_RETENTION_FREQ_MINUTES, :value => 5) 

    get :index

    assert_equal 100, assigns(:retention_time)
    assert_equal 5, assigns(:retention_frequency)
    assert_nil assigns(:last_run)
  end

  should "correctly set last_run if set in server_values" do
    timestamp = Time.now.to_i
    ServerValue.make(:type => 'message_retention_last_performed', :value => timestamp)
    
    get :index

    assert_equal timestamp, assigns(:last_run)
  end

end
