require 'test/unit'
require File.expand_path(File.join(File.dirname(__FILE__),'..','..','test_helper')) 

class NewRelic::Agent::WorkerLoopTest < Test::Unit::TestCase
  def setup
    @log = ""
    @logger = Logger.new(StringIO.new(@log))
    @worker_loop = NewRelic::Agent::WorkerLoop.new
    @worker_loop.stubs(:log).returns(@logger)
    @test_start_time = Time.now
  end

  def test_add_task
    @x = false
    @worker_loop.run(1) do
      @worker_loop.stop
      @x = true
      sleep 1
    end
    assert @x
  end

  def test_density
    # This shows how the tasks stay aligned with the period and don't drift.
    count = 0
    start = Time.now
    @worker_loop.run(1.0) do
      count +=1
      if count == 10
        @worker_loop.stop
        next
      end
      sleep 0.50
    end
    elapsed = Time.now - start
    assert_in_delta 10.2, elapsed, 0.2
  end
  def test_task_error__standard
    stop = false
    @logger.expects(:error).twice
    @logger.expects(:debug).never
    # This loop task will run twice
    @worker_loop.run(0.2) do
      @worker_loop.stop if stop
      stop = true
      raise "Standard Error Test"
    end
    assert stop
    puts @log
  end

  def test_task_error__server
    @logger.expects(:error).never
    @logger.expects(:debug).once
    @worker_loop.run(0.2) do
      @worker_loop.stop
      raise NewRelic::Agent::ServerError, "Runtime Error Test"
    end
  end
end
