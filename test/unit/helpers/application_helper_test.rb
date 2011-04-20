require 'test_helper'

class ApplicationHelperTest < ActionView::TestCase
  context "time_to_formatted_s" do
    should "format time with milliseconds" do
      assert_equal "2011-04-20 11:40:57.146", time_to_formatted_s(Time.zone.at(1303299657.146426))
    end
  end
end
