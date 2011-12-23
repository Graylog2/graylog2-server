require 'test_helper'

class FilteredTermTest < ActiveSupport::TestCase
  context "with terms" do
    setup do
      FilteredTerm.make(:term => "thar")
      FilteredTerm.make(:term => "baz")
      FilteredTerm.expire_cache
    end

    should "be applied" do
      message = Message.parse_from_hash(:message => "foo thar bar baz")
      assert_equal "foo [FILTERED] bar [FILTERED]", message.message
    end

    should "work with selectors" do
      FilteredTerm.make(:term => 'password\s(.+)\s')
      message = Message.parse_from_hash(:message => "User login with password foobar failed")
      assert_equal "User login with [FILTERED]failed", message.message
    end

    should "be cached" do
      terms = FilteredTerm.all.to_a
      FilteredTerm.expects(:all).once.with().returns(terms)
      assert_equal "foo [FILTERED]", Message.parse_from_hash(:message => "foo thar").message
      assert_equal "bar [FILTERED]", Message.parse_from_hash(:message => "bar baz").message
    end
  end
end
