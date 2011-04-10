require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

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
