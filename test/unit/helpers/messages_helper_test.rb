require 'test_helper'

class MessagesHelperTest < ActionView::TestCase
  def assert_equal_and_safe(expected, actual)
    assert_equal expected, actual
    assert actual.html_safe?, actual.inspect + ' is not html safe'
  end

  should "wrap long additional fields with <pre>" do
    assert_equal_and_safe "", format_additional_field_value('backtrace', nil)
    assert_equal_and_safe "9001", format_additional_field_value('bar', 9001)
    assert_equal_and_safe "backtrace", format_additional_field_value('backtrace', "backtrace")
    assert_equal_and_safe "<pre>backtrace\nline 1</pre>", format_additional_field_value('backtrace', "backtrace\nline 1")
  end

  test "get_quickfilter_selected gives back original value when no converting is set" do
    assert_equal "foo", get_quickfilter_selected({:message => "foo"}, :message)
  end

  test "get_quickfilter_selected returns nil when provided empty values" do
    assert_nil get_quickfilter_selected(Hash.new, :foo)
    assert_nil get_quickfilter_selected({:bar => "baz"}, :foo)
  end

  test "get_quickfilter_selected gives back original value when invalid converting type is provided" do
    assert_equal "foo", get_quickfilter_selected({:message => "foo"}, :message, :something_invalid)
  end

  test "get_quickfilter_selected gives back integer when requested" do
    assert_equal 42, get_quickfilter_selected({:message => "42"}, :message, :integer)
  end

  test "get_quickfilter_selected gives back BSON::ObjectId when requested" do
    id = BSON::ObjectId.new
    assert_equal id, get_quickfilter_selected({:some_id => id.to_s}, :some_id, :bson_id)
  end

end
