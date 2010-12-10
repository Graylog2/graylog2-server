#require 'test_helper'
require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class FavoritedStreamTest < ActiveSupport::TestCase

  test "find all favorited streams of a user" do
    FavoritedStream.make( { :stream_id => 1, :user_id => 1 }).save
    FavoritedStream.make( { :stream_id => 23, :user_id => 1 }).save
    FavoritedStream.make( { :stream_id => 3, :user_id => 2 }).save

    favorites = FavoritedStream.all_of_user(1)

    assert_equal 2, favorites.count
    assert_equal 1, favorites[0].stream_id
    assert_equal 23, favorites[1].stream_id
  end

end
