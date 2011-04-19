require 'test_helper'

class ApplicationHelperTest < ActionView::TestCase
  context "time_to_formatted_s" do
    should "format time with milliseconds" do
      assert_equal "2011-04-19 21:25:14.128", time_to_formatted_s(Time.at(1303233914.12842))
    end
  end
end
