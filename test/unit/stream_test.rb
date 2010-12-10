#require 'test_helper'
require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class StreamTest < ActiveSupport::TestCase
  
  test "favorites are deleted with stream" do
    stream_id = 15
    Stream.make(:id => stream_id).save

    # Create favorites.
    FavoritedStream.make(:stream_id => stream_id, :user_id => 1).save
    FavoritedStream.make(:stream_id => stream_id, :user_id => 2).save
    FavoritedStream.make(:stream_id => stream_id, :user_id => 3).save

    # Delete stream.
    Stream.find(stream_id).destroy

    # Check that favorites are deleted.
    assert FavoritedStream.find_by_stream_id(stream_id).blank? == true
  end

end
