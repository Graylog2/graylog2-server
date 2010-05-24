require File.expand_path(File.join(File.dirname(__FILE__),'/../test_helper'))
require File.expand_path(File.join(File.dirname(__FILE__),'/../../lib/new_relic/command'))
require 'rubygems'
require 'mocha'

class NewRelic::DeploymentsTest < Test::Unit::TestCase
  
  def setup
    NewRelic::Commands::Deployments.class_eval do
      attr_accessor :messages, :exit_status, :errors, :revision
      def err(message); @errors = @errors ? @errors + message : message; end
      def info(message); @messages = @messages ? @messages + message : message; end
      def just_exit(status=0); @exit_status ||= status; end
    end
  end
  def teardown
    return unless @deployment
    puts @deployment.errors
    puts @deployment.messages
    puts @deployment.exit_status
  end
  def test_help
    begin
      NewRelic::Commands::Deployments.new "-h"
      fail "should have thrown"
    rescue NewRelic::Commands::CommandFailure => c
      assert_match /^Usage/, c.message
    end
  end
  def test_bad_command
    assert_raise OptionParser::InvalidOption do
      NewRelic::Commands::Deployments.new ["-foo", "bar"]
    end
  end
  def test_interactive
    mock_the_connection
    @deployment = NewRelic::Commands::Deployments.new :appname => 'APP', :revision => 3838, :user => 'Bill', :description => "Some lengthy description"
    assert_nil @deployment.exit_status
    assert_nil @deployment.errors
    assert_equal '3838', @deployment.revision
    @deployment.run
    @deployment = nil
  end
  
  def test_command_line_run
    mock_the_connection
    #    @mock_response.expects(:body).returns("<xml>deployment</xml>")
    @deployment = NewRelic::Commands::Deployments.new(%w[-a APP -r 3838 --user=Bill] << "Some lengthy description")
    assert_nil @deployment.exit_status
    assert_nil @deployment.errors
    assert_equal '3838', @deployment.revision
    @deployment.run
    
    # This should pass because it's a bogus deployment
    #assert_equal 1, @deployment.exit_status
    #assert_match /Unable to upload/, @deployment.errors
    
    @deployment = nil
  end
  private
  def mock_the_connection
    mock_connection = mock()
    @mock_response = mock()
    @mock_response.expects(:is_a?).with(Net::HTTPSuccess).returns(true)
    mock_connection.expects(:request).returns(@mock_response)
    NewRelic::Control.instance.stubs(:http_connection).returns(mock_connection)
  end
  
end
