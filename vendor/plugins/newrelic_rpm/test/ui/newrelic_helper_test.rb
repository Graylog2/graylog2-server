require File.expand_path(File.join(File.dirname(__FILE__),'..','test_helper')) 
require 'newrelic_helper'
require 'active_record_fixtures'

class NewRelic::Agent::NewrelicHelperTest < Test::Unit::TestCase
  include NewrelicHelper
  
  def params
    {}
  end
  
  def setup
    super
    ActiveRecordFixtures.setup
    # setup instrumentation
    NewRelic::Agent.manual_start 
    # let's get a real stack trace
    begin
      ActiveRecordFixtures::Order.find 0
    rescue => e
      @exception = e
      return
    end
    flunk "should throw"
  end
  def teardown
    ActiveRecordFixtures.teardown
    NewRelic::Agent.instance.shutdown
    super
  end
  def test_application_caller
    assert_match /setup/, application_caller(@exception.backtrace)
  end
  
  def test_application_stack_trace__rails
    assert_clean(application_stack_trace(@exception.backtrace, true), true)
  end
  def test_application_stack_trace__no_rails
    assert_clean(application_stack_trace(@exception.backtrace, false), false)
  end 
  def test_with_delimiter
    assert_equal "123,456.123456", with_delimiter(123456.123456)
  end
  
  private
  def assert_clean(backtrace, rails=false)
    if !rails
      assert_equal 0, backtrace.grep('/rails/').size, backtrace.join("\n")
    end
    assert_equal 0, backtrace.grep(/trace/).size, backtrace.join("\n")
    assert_equal 0, backtrace.grep(/newrelic_rpm\/agent/).size, backtrace.join("\n")
  end
end