require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class UserTest < ActiveSupport::TestCase
  include AuthenticatedTestHelper

  setup do
    @user = User.make
  end

  context "with stream" do
    setup do
      @stream = Stream.make
    end

    %w(streams favorite_streams subscribed_streams).each do |rel|
      should "have #{rel} association" do
        @user.send(rel + '=', [@stream])
        @user.save!
        assert_equal [@stream], @user.send(rel)
      end
    end
  end

  def test_should_create_user
    assert_difference 'User.count' do
      user = User.make
      assert !user.new_record?, "#{user.errors.full_messages.to_sentence}"
    end
  end

  def test_should_require_login
    assert_no_difference 'User.count' do
      u = User.make_unsaved(:login => nil)
      assert_not_nil u.errors[:login]
    end
  end

  def test_should_require_password
    assert_no_difference 'User.count' do
      u = User.make_unsaved(:password => nil)
      assert_not_nil u.errors[:password]
    end
  end

  def test_should_require_password_confirmation
    assert_no_difference 'User.count' do
      u = User.make_unsaved(:password_confirmation => nil)
      assert_not_nil u.errors[:password_confirmation]
    end
  end

  def test_should_require_email
    assert_no_difference 'User.count' do
      u = User.make_unsaved(:email => nil)
      assert_not_nil u.errors[:email]
    end
  end

  def test_should_reset_password
    @user.update_attributes(:password => 'new password', :password_confirmation => 'new password')
    assert_equal @user, User.authenticate(@user.login, @user.password)
  end

  def test_should_not_rehash_password
    @user.update_attributes(:login => 'quentin2')
    assert_equal @user, User.authenticate('quentin2', @user.password)
  end

  def test_should_authenticate_user
    assert_equal @user, User.authenticate(@user.login, @user.password)
  end

  def test_should_set_remember_token
    @user.remember_me
    assert_not_nil @user.remember_token
    assert_not_nil @user.remember_token_expires_at
  end

  def test_should_unset_remember_token
    @user.remember_me
    assert_not_nil @user.remember_token
    @user.forget_me
    assert_nil @user.remember_token
  end

  def test_should_remember_me_for_one_week
    before = 1.week.from_now.utc
    @user.remember_me_for 1.week
    after = 1.week.from_now.utc
    assert_not_nil @user.remember_token
    assert_not_nil @user.remember_token_expires_at
    assert @user.remember_token_expires_at.between?(before, after)
  end

  def test_should_remember_me_until_one_week
    time = 1.week.from_now.utc
    @user.remember_me_until time
    assert_not_nil @user.remember_token
    assert_not_nil @user.remember_token_expires_at
    assert_equal @user.remember_token_expires_at, time
  end

  def test_should_remember_me_default_two_weeks
    before = 2.weeks.from_now.utc
    @user.remember_me
    after = 2.weeks.from_now.utc
    assert_not_nil @user.remember_token
    assert_not_nil @user.remember_token_expires_at
    assert @user.remember_token_expires_at.between?(before, after)
  end

end
