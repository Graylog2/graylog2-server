require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class MessageTest < ActiveSupport::TestCase
  should "test all_of_hostgroup"

  context "with message" do
    setup do
      @message = Message.make
    end

    Message::FIELDS.each do |field|
      _field = "_#{field}"

      should "respond to #{field} and #{field}=" do
        assert_respond_to(@message, field)
        assert_respond_to(@message, field + '=')
      end

      should "respond to #{_field} and #{_field}=" do
        assert_respond_to(@message, _field)
        assert_respond_to(@message, _field + '=')
      end

      should "implement #{field} via #{_field}" do
        @message.expects(_field)
        @message.__send__(field)
      end

      should "implement #{field}= via #{_field}=" do
        @message.expects(_field + '=')
        value = @message.__send__(_field)
        @message.__send__(field + '=', value)
      end
    end
  end

  should "test count_of_hostgroup" do
    Message.make(:host => "foobar", :message => "bla").save
    Message.make(:host => "foobarish", :message => "gdfgdfhh").save
    Message.make(:host => "foofoo", :message => "foobarish").save
    Message.make(:host => "somehost", :message => "gdfgfdd").save
    Message.make(:host => "somehost", :message => "wat").save
    Message.make(:host => "anotherhost", :message => "foobar").save
    Message.make(:host => "anotherfoohost", :message => "don't match me").save
    
    hostgroup = Hostgroup.find(3)
    assert_equal 5, Message.count_of_hostgroup(hostgroup)
  end
end
