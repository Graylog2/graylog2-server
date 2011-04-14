require 'test_helper'

class MessagesHelperTest < ActionView::TestCase

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
