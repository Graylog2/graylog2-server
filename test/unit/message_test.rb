require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class MessageTest < ActiveSupport::TestCase
  should "test all_of_hostgroup"

  should "test count_of_hostgroup" do
    Host.make(:host => "somehost").save

    Message.make(:host => "foobar", :message => "bla").save
    Message.make(:host => "foobarish", :message => "gdfgdfhh").save
    Message.make(:host => "foofoo", :message => "foobarish").save
    Message.make(:host => "somehost", :message => "gdfgfdd").save
    Message.make(:host => "somehost", :message => "wat").save
    Message.make(:host => "anotherhost", :message => "foobar").save
    Message.make(:host => "anotherfoohost", :message => "don't match me").save

    hostgroup = Hostgroup.make
    HostgroupHost.make(:hostname => /^foo/, :ruletype => HostgroupHost::TYPE_REGEX, :hostgroup_id => hostgroup.id)
    HostgroupHost.make(:hostname => "somehost", :ruletype => HostgroupHost::TYPE_SIMPLE, :hostgroup_id => hostgroup.id)
    HostgroupHost.make(:hostname => /^another/, :ruletype => HostgroupHost::TYPE_REGEX, :hostgroup_id => hostgroup.id)
    assert_equal 7, Message.count_of_hostgroup(hostgroup)
  end

  should "find additional fields" do
    Message.make(:host => "local", :message => "hi!", :_foo => "bar").save
    message = Message.last
    assert message.has_additional_fields
    expected = [{:value => 'bar', :key => 'foo' }]
    assert_equal expected, message.additional_fields
  end

  should "correctly paginate" do
    (Message::LIMIT+10).times { Message.make }
    assert_equal Message::LIMIT, Message.all_paginated(1).count
    assert_equal 10, Message.all_paginated(2).count
  end

  should "corretly paginate when no page count is given" do
    (Message::LIMIT+10).times { Message.make }
    assert_equal Message::LIMIT, Message.all_paginated().count
    assert_equal 10, Message.all_paginated(2).count
  end
end
