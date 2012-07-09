require 'test_helper'

class MessageTest < ActiveSupport::TestCase
  should "have few time fields" do
    message = Message.parse_from_hash({:created_at => Time.now.to_f})

    assert_kind_of(Float, message.created_at)
    assert !message.respond_to?(:timestamp), "Do not define method like this."
    assert !message.respond_to?(:date), "Do not define method like this."
    assert !message.respond_to?(:time), "Do not define method like this."
  end

  context "creation time" do
    should "be returned in request's timezone" do
      message = Message.new()
      message.created_at = Time.now.to_i

      assert_in_delta(Time.now.utc.to_f, message.created_at, 3.0)

      assert_equal 'UTC', Time.zone.name, "Please do not change default timezone"
      assert_equal Time.zone.name, message.created_at_time.zone
      assert_equal Time.zone.at(message.created_at), message.created_at_time
    end
  end

  should "always return message" do
    message = Message.parse_from_hash(:message => nil)  # due to a bug in server, for example
    assert_equal '', message.message
  end

  should "return file and line without absent values" do
    assert_equal 'foo.rb:42', Message.parse_from_hash(:file => 'foo.rb', :line => 42).file_and_line
    assert_equal 'foo.rb',    Message.parse_from_hash(:file => 'foo.rb', :line => nil).file_and_line
    assert_equal '',          Message.parse_from_hash(:file => nil,      :line => nil).file_and_line
  end

  should "find additional fields" do
    message = Message.parse_from_hash(:host => "local", :message => "hi!", :_foo => "bar", :_baz => "1", :invalid => "123")
    assert message.additional_fields?
    assert_equal({'foo' => 'bar', 'baz' => '1'}, message.additional_fields)
  end

  should "correctly paginate" do
    (Message::LIMIT+10).times { bm }
    assert_equal Message::LIMIT, MessageGateway.all_paginated(1).count
    assert_equal 10, MessageGateway.all_paginated(2).count
  end

  should "correctly paginate when no page count is given" do
    (Message::LIMIT+10).times { bm }
    assert_equal Message::LIMIT, MessageGateway.all_paginated().count
    assert_equal 10, MessageGateway.all_paginated(2).count
  end
end
