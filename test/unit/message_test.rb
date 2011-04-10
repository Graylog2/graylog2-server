require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class MessageTest < ActiveSupport::TestCase
  setup do
    @stream = Stream.make
  end

  test "find by id" do
    id = @stream.id

    assert_equal @stream, Stream.find(id)
    assert_equal @stream, Stream.find(id.to_s)
    assert_equal @stream, Stream.find_by_id(id.to_s)

    assert_raise(TypeError) { Stream.find_by_id(id) }
  end

  should "test count_of_hostgroup" do
    Host.make(:host => "somehost").save

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

  should "find additional fields" do
    Message.make(:host => "local", :message => "hi!", :_foo => "bar").save
    message = Message.last
    assert message.has_additional_fields
    expected = [{:value => 'bar', :key => 'foo' }]
    assert_equal expected, message.additional_fields
  end
end
