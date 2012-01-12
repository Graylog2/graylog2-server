require 'test_helper'

class HostTest < ActiveSupport::TestCase
  should validate_presence_of(:host)
end
