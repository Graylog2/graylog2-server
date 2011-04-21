require 'test_helper'

class MessagesHelperTest < ActionView::TestCase
  def assert_equal_and_safe(expected, actual)
    assert_equal expected, actual
    assert actual.html_safe?, actual.inspect + ' is not html safe'
  end

  should "wrap long additional fields with <pre>" do
    assert_equal_and_safe "", format_additional_field_value('backtrace', nil)
    assert_equal_and_safe "backtrace", format_additional_field_value('backtrace', "backtrace")
    assert_equal_and_safe "<pre>backtrace\nline 1</pre>", format_additional_field_value('backtrace', "backtrace\nline 1")
  end
end
