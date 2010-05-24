require File.expand_path(File.join(File.dirname(__FILE__),'..','test_helper')) 
require 'newrelic_helper'
require 'active_record_fixtures'

class NewRelic::Agent::NewrelicControllerTest < Test::Unit::TestCase

  # Clearly we need a functional test for the controller.  For now
  # I want to at least make sure the class loads in all versions of rails.
  def test_controller_loading
    NewrelicController
  rescue
    fail "Controller would not load:#{$!}"
  end
end