require 'test_helper'
require File.expand_path('../../../realtime/lib/channel_manager.rb', __FILE__)

class WebSocketStub

  def initialize(method, path)
    @method = method
    @path = path
  end

  def request
    { "method" => @method, "path" => @path }
  end

end

class WebsocketServerChannelManagerTest < ActiveSupport::TestCase

  should "register client to overall channel" do
    m = ChannelManager.new
    m.register_client(WebSocketStub.new("GET", "/overall"))
    
    assert_equal Hash.new, m.channels[:streams]
    assert_equal EventMachine::Channel, m.channels[:overall].class
  end

  should "register multiple clients to overall channel" do
    m = ChannelManager.new
    m.register_client(WebSocketStub.new("GET", "/overall"))
    m.register_client(WebSocketStub.new("GET", "/overall"))
    
    assert_equal Hash.new, m.channels[:streams]
    assert_equal EventMachine::Channel, m.channels[:overall].class
  end

  should "successfully unregister client from overall channel" do
    m = ChannelManager.new
    uid = m.register_client(WebSocketStub.new("GET", "/overall"))
  
    assert_nothing_raised do
      m.unregister_client(uid)
    end
  end

  should "successfully register client to stream channel" do
    stream_id = BSON::ObjectId.new.to_s
    m = ChannelManager.new
    m.register_client(WebSocketStub.new("GET", "/stream/#{stream_id}"))

    assert_equal EventMachine::Channel, m.channels[:streams][stream_id].class
  end

  should "successfully unregister client from stream channel" do
    stream_id = BSON::ObjectId.new.to_s
    m = ChannelManager.new
    uid = m.register_client(WebSocketStub.new("GET", "/stream/#{stream_id}"))
    
    assert_nothing_raised do
      m.unregister_client(uid)
    end
  end

  should "work with trailing slash behind stream id" do
    stream_id = BSON::ObjectId.new.to_s
    m = ChannelManager.new
    m.register_client(WebSocketStub.new("GET", "/stream/#{stream_id}/"))

    assert_equal EventMachine::Channel, m.channels[:streams][stream_id].class
  end

  should "successfully register multiple clients to difference stream channels" do
    stream_ids = []
    7.times { stream_ids << BSON::ObjectId.new.to_s }
    m = ChannelManager.new

    stream_ids.each do |stream_id|
      m.register_client(WebSocketStub.new("GET", "/stream/#{stream_id}/"))
    end

    stream_ids.each do |stream_id|
      assert_equal EventMachine::Channel, m.channels[:streams][stream_id].class
    end
  end

  should "succcessfully unregister client from multiple stream channels" do
    stream_ids = []
    7.times { stream_ids << BSON::ObjectId.new.to_s }
    m = ChannelManager.new

    uids = []
    stream_ids.each do |stream_id|
      uids << m.register_client(WebSocketStub.new("GET", "/stream/#{stream_id}/"))
    end

    assert_nothing_raised do
      uids.each do |uid|
        m.unregister_client(uid)
      end
    end
  end

  should "not subscribe if HTTP verb is invalid" do
    m = ChannelManager.new

    assert_raises InvalidHTTPVerbException do
      m.register_client(WebSocketStub.new("POST", "/overall"))
    end
  end

  should "not subscribe if URI is target type" do
    m = ChannelManager.new

    assert_raises InvalidPathException do
      m.register_client(WebSocketStub.new("GET", "/foo/bar"))
    end
  end

end
