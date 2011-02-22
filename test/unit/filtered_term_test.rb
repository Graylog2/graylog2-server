require 'test_helper'

class FilteredTermTest < ActiveSupport::TestCase
  test "should actually filter out the term" do
    FilteredTerm.make(:term => "thar").save
    msg = Message.make(:message => "foo thar foo").message

    assert_equal "foo [FILTERED] foo", msg
  end
end
