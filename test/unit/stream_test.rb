require 'test_helper'

class StreamTest < ActiveSupport::TestCase
  setup do
    @stream = Stream.make
  end

  context "with user" do
    setup do
      @user = User.make
    end

    %w(users favorited_streams subscribers).each do |rel|
      should "have #{rel} association" do
        @stream.send(rel + '=', [@user])
        @stream.save!
        assert_equal [@user], @stream.send(rel)
      end
    end

    should "have additional_columns field" do
      stream = Stream.make
      assert_equal stream.additional_columns, []
    end

    context "disabling streams" do

      should "append disabled hint after title if stream is disabled" do
        @stream.disabled = true
        assert_equal "#{@stream.title} (disabled)", @stream.title_possibly_disabled
      end

      should "not append disabled hint after title if stream is not disabled" do
        @stream.disabled = false
        assert_equal @stream.title, @stream.title_possibly_disabled
      end

      should "possibly_disabled title should return nil if original title is nil" do
        @stream.title = nil
        assert_nil @stream.title_possibly_disabled
      end

    end
  end

  context "related streams" do

    should "find related streams" do
      related_streams = Array.new
      titles = ["foo9001", "foo9002", "foo9003", "foo9004"]
      titles.each { |t| related_streams << Stream.make(:title => t) }
      stream = Stream.make(:related_streams_matcher => '^foo\d{4}')
      assert_equal related_streams.map{ |s| s.id }, stream.related_streams.map { |s| s.id }
    end

    should "return empty array if no matcher is set" do
      titles = ["foo9001", "foo9002", "foo9003", "foo9004"]
      titles.each { |t| Stream.make(:title => t) }
      stream = Stream.make(:related_streams_matcher => nil)
      assert_equal [], stream.related_streams.map { |s| s.id }
    end

    should "not include itself in matches" do
      Stream.make(:title => "foo9001")
      stream = Stream.make(:title => "foo9002", :related_streams_matcher => '^foo\d{4}')
      assert !stream.related_streams.map{ |s| s.id }.include?(stream.id)
    end

  end

  context "alarm status" do

    should "mark alarms as disabled if alarms are disabled" do
      stream = Stream.make(:alarm_active => false, :alarm_force => true)
      assert_equal :disabled, stream.alarm_status(User.make)
    end

    should "mark alarms as disabled if alarms are enabled but no alarm limit is set" do
      stream = Stream.make(:alarm_active => true, :alarm_limit => nil, :alarm_force => true)
      assert_equal :disabled, stream.alarm_status(User.make)
    end

    should "mark alarms as disabled if alarms are enabled but no alarm timespan is set" do
      stream = Stream.make(:alarm_active => true, :alarm_timespan => nil, :alarm_force => true)
      assert_equal :disabled, stream.alarm_status(User.make)
    end

    should "mark alarms as disabled if user has not enabled alarms for this stream" do
      stream = Stream.make(:alarm_active => true, :alarm_timespan => 10, :alarm_limit => 10, :alarm_force => false)
      assert_equal :disabled, stream.alarm_status(User.make)
    end

    should "not mark alarms as disabled if user has not enabled alarms for this stream but alarm force is active" do
      stream = Stream.make(:alarm_active => true, :alarm_timespan => 10, :alarm_limit => 10, :alarm_force => true)
      assert_equal :no_alarm, stream.alarm_status(User.make)
    end

    should "not mark alarms as disabled if user has enabled alarm for this stream and alarm_force is not active" do
      user = User.make
      stream = Stream.make(:alarm_active => true, :alarm_timespan => 10, :alarm_limit => 10, :alarm_force => false)
      AlertedStream.make(:stream_id => stream.id, :user_id => user.id)
      assert_equal :no_alarm, stream.alarm_status(user)
    end

    should "mark stream as not alarmed if under limit" do
      stream = Stream.make(:alarm_active => true, :alarm_limit => 5, :alarm_timespan => 10, :alarm_force => true)
      4.times { Message.make(:streams => stream.id) }

      assert_equal :no_alarm, stream.alarm_status(User.make)
    end

    should "mark stream as alarmed if over limit" do
      stream = Stream.make(:alarm_active => true, :alarm_limit => 10, :alarm_timespan => 10, :alarm_force => true)
      8.times { Message.make(:streams => stream.id) }
      5.times { Message.make(:streams => [ stream.id, Stream.make.id ]) } # this goes over the alarm_limit

      assert_equal :alarm, stream.alarm_status(User.make)
    end

  end

# Disable for now.
#
#  test "favorites are deleted with stream" do
#    stream_id = 15
#    Stream.make(:id => stream_id).save
#
#    # Create favorites.
#    FavoritedStream.make(:stream_id => stream_id, :user_id => 1).save
#    FavoritedStream.make(:stream_id => stream_id, :user_id => 2).save
#    FavoritedStream.make(:stream_id => stream_id, :user_id => 3).save
#
#    # Delete stream.
#    Stream.find(stream_id).destroy
#
#    # Check that favorites are deleted.
#    assert FavoritedStream.find_by_stream_id(stream_id).blank? == true
#  end
#
#  test "find all favorited streams of a user" do
#    FavoritedStream.make( { :stream_id => 1, :user_id => 1 }).save
#    FavoritedStream.make( { :stream_id => 23, :user_id => 1 }).save
#    FavoritedStream.make( { :stream_id => 3, :user_id => 2 }).save
#
#    favorites = FavoritedStream.all_of_user(1)
#
#    assert_equal 2, favorites.count
#    assert_equal 1, favorites[0].stream_id
#    assert_equal 23, favorites[1].stream_id
#  end

end
