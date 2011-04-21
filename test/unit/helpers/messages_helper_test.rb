require 'test_helper'

class MessagesHelperTest < ActionView::TestCase
  def assert_equal_and_safe(expected, actual)
    assert_equal expected, actual
    assert actual.html_safe?, actual.inspect + ' is not html safe'
  end

  should "wrap backtraces with <pre>" do
    assert_equal_and_safe 'abc', format_additional_field_value('something', 'abc')
    assert_equal_and_safe '<pre>backtrace</pre>', format_additional_field_value('backtrace', 'backtrace')
    assert_equal_and_safe '<pre>stacktrace</pre>', format_additional_field_value('stacktrace', 'stacktrace')
    assert_equal_and_safe '<pre>traceback</pre>', format_additional_field_value('traceback', 'traceback')
  end
end
