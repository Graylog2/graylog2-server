require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class SettingsControllerTest < ActionController::TestCase

  should "store a setting" do
    assert_equal Setting::TYPE_MESSAGE_LENGTH_STANDARD, Setting.get_message_length(@logged_in_user)
    post :store, :setting_type => Setting::TYPE_MESSAGE_LENGTH, :value => 10
    assert_redirected_to :controller => :settings
    assert_equal 10, Setting.get_message_length(@logged_in_user)
  end

  should "redirect to retentiontime controller for some setting types" do
    assert_equal Setting::TYPE_RETENTION_TIME_DAYS_STANDARD, Setting.get_retention_time_days(@logged_in_user)
    post :store, :setting_type => Setting::TYPE_RETENTION_TIME_DAYS, :value => 9001
    assert_redirected_to :controller => :retentiontime
    assert_equal 9001, Setting.get_retention_time_days(@logged_in_user)
  end

end
