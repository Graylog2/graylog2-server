require 'test_helper'

class ApplicationHelperTest < ActionView::TestCase
  context "time_to_formatted_s" do
    should "format time with milliseconds" do
      res = time_to_formatted_s(Time.zone.at(1303299657.146426))
      assert res.html_safe?
      assert_equal "2011-04-20 11:40:57<span class='time-light'>.146</span>", res
    end
  end
end
