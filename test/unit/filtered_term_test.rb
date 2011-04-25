require 'test_helper'

class FilteredTermTest < ActiveSupport::TestCase
  context "with terms" do
    setup do
      FilteredTerm.make(:term => "thar")
      FilteredTerm.make(:term => "baz")
      FilteredTerm.expire_cache
    end

    should "be applied" do
      message = Message.make(:message => "foo thar bar baz")
      assert_equal "foo [FILTERED] bar [FILTERED]", message.message
    end

    should "be cached" do
      terms = FilteredTerm.all.to_a
      FilteredTerm.expects(:all).once.with().returns(terms)
      assert_equal "foo [FILTERED]", Message.make(:message => "foo thar").message
      assert_equal "bar [FILTERED]", Message.make(:message => "bar baz").message
    end
  end
end
