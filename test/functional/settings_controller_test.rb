require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class SettingsControllerTest < ActionController::TestCase

  should "store a setting" do
    assert_equal Setting::TYPE_MESSAGE_LENGTH_STANDARD, Setting.get_message_length(@logged_in_user)
    post :store, :setting_type => Setting::TYPE_MESSAGE_LENGTH, :value => 10
    assert_redirected_to :controller => :settings
    assert_equal 10, Setting.get_message_length(@logged_in_user)
  end

  context "columns" do
    
    should "add a new column" do
      assert_equal Setting::TYPE_ADDITIONAL_COLUMNS_STANDARD, Setting.get_additional_columns(@logged_in_user)
      post :store, :setting_type => Setting::TYPE_ADDITIONAL_COLUMNS, :value => "MAMA"
      
      assert_not_nil flash[:notice]
      assert_redirected_to :controller => :additionalcolumns
      assert_equal ["MAMA"], Setting.get_additional_columns(@logged_in_user)
    end
    
    should "not add an already existing column" do
      assert_equal Setting::TYPE_ADDITIONAL_COLUMNS_STANDARD, Setting.get_additional_columns(@logged_in_user)
      post :store, :setting_type => Setting::TYPE_ADDITIONAL_COLUMNS, :value => "MAMA"
      post :store, :setting_type => Setting::TYPE_ADDITIONAL_COLUMNS, :value => "MAMA"
      
      assert_not_nil flash[:error]
      assert_redirected_to :controller => :additionalcolumns
      assert_equal [ "MAMA" ], Setting.get_additional_columns(@logged_in_user)
    end
    
    should "not add an empty column" do
      assert_equal Setting::TYPE_ADDITIONAL_COLUMNS_STANDARD, Setting.get_additional_columns(@logged_in_user)
      post :store, :setting_type => Setting::TYPE_ADDITIONAL_COLUMNS, :value => ""
      
      assert_not_nil flash[:error]
      assert_redirected_to :controller => :additionalcolumns
      assert_equal Setting::TYPE_ADDITIONAL_COLUMNS_STANDARD, Setting.get_additional_columns(@logged_in_user)
    end
    
    should "remove an existing column" do
      assert_equal Setting::TYPE_ADDITIONAL_COLUMNS_STANDARD, Setting.get_additional_columns(@logged_in_user)
      
      setting = Setting.make
      setting.user_id = @logged_in_user.id
      setting.setting_type = Setting::TYPE_ADDITIONAL_COLUMNS
      setting.value = ["MAMA"]
      setting.save!
      
      delete :removecolumn, :id => setting.id, :column => "MAMA"
      
      assert_not_nil flash[:notice]
      assert_redirected_to :controller => :additionalcolumns
      assert_equal Setting::TYPE_ADDITIONAL_COLUMNS_STANDARD, Setting.get_additional_columns(@logged_in_user)
    end
    
    should "not remove a non-existent column" do
      assert_equal Setting::TYPE_ADDITIONAL_COLUMNS_STANDARD, Setting.get_additional_columns(@logged_in_user)
      
      result = ["MAMA"]
      
      setting = Setting.make
      setting.user_id = @logged_in_user.id
      setting.setting_type = Setting::TYPE_ADDITIONAL_COLUMNS
      setting.value = result
      setting.save!
      
      delete :removecolumn, :id => setting.id, :column => "PAPA"
      
      assert_not_nil flash[:error]
      assert_redirected_to :controller => :additionalcolumns
      assert_equal result, Setting.get_additional_columns(@logged_in_user)
    end
    
  end

end
