require File.expand_path(File.dirname(__FILE__) + "/../test_helper")

class HostTest < ActiveSupport::TestCase
  should validate_presence_of(:host)
end
