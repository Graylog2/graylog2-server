require 'test_helper'

class StreamruleTest < ActiveSupport::TestCase

  INVALID_REGEX = "[*"
  VALID_REGEX = "^foo{3}.$"

  context "regex verfication" do

    should "not accept invalid regexes" do
      stream = Stream.make
      stream.streamrules << Streamrule.new(:rule_type => Streamrule::TYPE_MESSAGE, :value => INVALID_REGEX)
      assert !stream.save
    end

    should "accept valid regexes" do
      stream = Stream.make
      stream.streamrules << Streamrule.new(:rule_type => Streamrule::TYPE_MESSAGE, :value => VALID_REGEX)
      assert stream.save
    end

    should "not check for valid regexes on fields that are marked as non-regex" do
      stream = Stream.make
      stream.streamrules << Streamrule.new(:rule_type => Streamrule::TYPE_SEVERITY, :value => INVALID_REGEX)
      assert stream.save
    end

  end

end
