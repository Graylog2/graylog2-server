require 'test_helper'

class OperationInterfaceTest < ActiveSupport::TestCase

  should "correctly return active queries" do
    stub = Hash.new
    stub["inprog"] = Array.new
    stub["inprog"] << generate_stub(:find)
    stub["inprog"] << generate_stub(:count)
    stub["inprog"] << generate_stub(:distinct)
    OperationInterface.any_instance.stubs(:ops).returns(stub)
    
    oi = OperationInterface.new

    expected = [
      {:opid=>845, :secs_running=>369, :type=>"find", :query=>{"foo"=>"bar"}},
      {:opid=>864, :secs_running=>4, :type=>"count", :query=>{"foo"=>"bar"}},
      {:opid=>868, :secs_running=>2, :type=>"distinct", :query=>{}}
    ]

    assert_equal expected, oi.get_all
  end

  should "skip queries of foreign collections or databases" do
    op = generate_stub(:find)
    op["ns"] = "something_important.superwichtig"

    OperationInterface.any_instance.stubs(:ops).returns( { "inprog" => [ op, generate_stub(:count) ] } )
    
    oi = OperationInterface.new
    assert_equal "count", oi.get_all[0][:type]
    assert_equal 1, oi.get_all.count
  end

  should "skip queries which are not of type query" do
    op = generate_stub(:find)
    op["op"] = "zomg-indexgeneration"

    OperationInterface.any_instance.stubs(:ops).returns( { "inprog" => [ op, generate_stub(:distinct) ] } )
    
    oi = OperationInterface.new
    assert_equal "distinct", oi.get_all[0][:type]
    assert_equal 1, oi.get_all.count
  end

  should "skip queries wich are not performing an allowed operation" do
    op = generate_stub(:count)
    op["query"] = {"delete"=>"messages", "query"=>{"foo"=>"bar"}, "fields"=>nil}

    OperationInterface.any_instance.stubs(:ops).returns( { "inprog" => [ op, generate_stub(:find) ] } )
    
    oi = OperationInterface.new
    assert_equal "find", oi.get_all[0][:type]
    assert_equal 1, oi.get_all.count
  end

  should "correctly count active queries" do
    stub = Hash.new
    stub["inprog"] = Array.new
    stub["inprog"] << generate_stub(:find)
    stub["inprog"] << generate_stub(:distinct)
    OperationInterface.any_instance.stubs(:ops).returns(stub)
    
    oi = OperationInterface.new
    assert_equal 2, oi.count
  end

  should "not kill operations which it is not allowed to" do
    OperationInterface.any_instance.stubs(:allowed_op?).returns(false)
    oi = OperationInterface.new
    assert !oi.kill(9001)
  end

  should "not fail at killing an operation" do
    assert_nothing_raised do
      OperationInterface.any_instance.stubs(:allowed_op?).returns(true)
      oi = OperationInterface.new
      assert oi.kill(9001)
    end
  end

  private
  def generate_stub(what)
    case what
      when :find then return {
        "opid"=>845,
        "active"=>true,
        "lockType"=>"read",
        "waitingForLock"=>false,
        "secs_running"=>369,
        "op"=>"query",
        "ns"=>"graylog2_web_interface_test.messages",
        "query"=>{"$query"=>{"foo"=>"bar"}, "$orderby"=>{"created_at"=>-1}},
        "client"=>"127.0.0.1:38047",
        "desc"=>"conn"
      }
      when :count then return {
        "opid"=>864,
        "active"=>true,
        "lockType"=>"read",
        "waitingForLock"=>false,
        "secs_running"=>4,
        "op"=>"query",
        "ns"=>"graylog2_web_interface_test.messages",
        "query"=>{"count"=>"messages", "query"=>{"foo"=>"bar"}, "fields"=>nil},
        "client"=>"127.0.0.1:47539",
        "desc"=>"conn"
      }
      when :distinct then return {
        "opid"=>868,
        "active"=>true,
        "lockType"=>"read",
        "waitingForLock"=>false,
        "secs_running"=>2,
        "op"=>"query",
        "ns"=>"graylog2_web_interface_test",
        "query"=>{"distinct"=>"messages", "key"=>"foo", "query"=>{}},
        "client"=>"127.0.0.1:47539",
        "desc"=>"conn"
      }
    end
  end

end
