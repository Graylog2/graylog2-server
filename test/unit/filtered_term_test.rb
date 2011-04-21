require 'test_helper'

class FilteredTermTest < ActiveSupport::TestCase
  should "be applied" do
    FilteredTerm.make(:term => "thar")
    FilteredTerm.make(:term => "baz")
    message = Message.make(:message => "foo thar bar baz")

    assert_equal "foo [FILTERED] bar [FILTERED]", message.message
  end
end
