require 'test_helper'

class StreamsControllerTest < ActionController::TestCase

  context "creating" do

    should "create and redirect" do
      assert_difference('Stream.count') do
        post :create, :stream => { :title => 'foo' }
      end

      assert_nil flash[:error]
      assert_redirected_to rules_stream_path(assigns(:new_stream))
    end

    should "be disabled from the beginning" do
      assert_difference('Stream.count') do
        post :create, :stream => { :title => 'foo' }
      end

      assert assigns(:new_stream).disabled
      assert_nil flash[:error]
      assert_redirected_to rules_stream_path(assigns(:new_stream))
    end

    should "redirect to stream index in case of error" do
      assert_no_difference('Stream.count') do
        post :create # no parameters
      end

      assert_not_nil flash[:error]
      assert_redirected_to streams_path
    end

  end

  context "enabling and disabling" do

    should "disable a stream that has no disabled attribute yet" do
      stream = Stream.make(:disabled => nil)
      post :toggledisabled, :id => stream.id.to_s

      assert_response :redirect
      assert assigns(:stream).disabled
    end

    should "disable a stream that is enabled" do
      stream = Stream.make(:disabled => false)
      post :toggledisabled, :id => stream.id.to_s

      assert_response :redirect
      assert assigns(:stream).disabled
    end

    should "enable a stream that is disabled" do
      stream = Stream.make(:disabled => true)
      post :toggledisabled, :id => stream.id.to_s

      assert_response :redirect
      assert !assigns(:stream).disabled
    end

  end

  context "cloning" do

    should "fail and redirect with error message when title parameter is missing" do
      stream = Stream.make
      post :clone, :id => stream.to_param
      assert_response :redirect
      assert_not_nil flash[:error]
    end

    should "clone" do
      stream = Stream.make
      stream.streamrules << Streamrule.new(:rule_type => 1, :value => /foo/)
      stream.save!

      post :clone, :id => stream.to_param, :title => "MAMA"
      assert_response :redirect

      assert_equal "MAMA", assigns(:new_stream).title
      assert_equal stream.streamrules, assigns(:new_stream).streamrules
    end

  end

  context "columns" do

    should "add a new column" do
      stream = Stream.make
      post :addcolumn, :id => stream.to_param, :column => "MAMA"
      assert_response :redirect
      assert_not_nil flash[:notice]

      assert_equal "MAMA", assigns(:stream).additional_columns.first
    end

    should "not add a new column twice" do
      stream = Stream.make
      post :addcolumn, :id => stream.to_param, :column => "MAMA"
      post :addcolumn, :id => stream.to_param, :column => "MAMA"

      assert_not_nil flash[:error]

      assert_equal 1, assigns(:stream).additional_columns.count
    end
    
    should "not add an empty column" do
      stream = Stream.make
      stream.additional_columns = []
      stream.save!
      
      post :addcolumn, :id => stream.to_param, :column => ""

      assert_not_nil flash[:error]

      assert_equal 0, assigns(:stream).additional_columns.count
    end

    should "remove a column" do
      stream = Stream.make
      stream.additional_columns << "MAMA"
      stream.save!

      delete :removecolumn, :id => stream.to_param, :column => "MAMA"

      assert_response :redirect
      assert_not_nil flash[:notice]

      assert_equal 0, assigns(:stream).additional_columns.count
    end

    should "remove a non-existant column" do
      stream = Stream.make
      stream.additional_columns << "MAMA"
      stream.save!

      delete :removecolumn, :id => stream.to_param, :column => "PAPA"

      assert_not_nil flash[:error]
    end

  end

  context "shortnames" do

    should "set a new shortname" do
      stream = Stream.make
      post :shortname, :id => stream.to_param, :shortname => "foo9001"

      assert_response :redirect
      assert_not_nil flash[:notice]

      assert_equal "foo9001", assigns(:stream).shortname
    end

    should "change an exiting shortname" do
      stream = Stream.make(:shortname => "foo")
      post :shortname, :id => stream.to_param, :shortname => "bar"

      assert_response :redirect
      assert_not_nil flash[:notice]

      assert_equal "bar", assigns(:stream).shortname
    end

    should "accept underscores in shortnames" do
      stream = Stream.make
      post :shortname, :id => stream.to_param, :shortname => "foo_9001"

      assert_response :redirect
      assert_not_nil flash[:notice]

      assert_equal "foo_9001", assigns(:stream).shortname
    end

    should "only accept alphanumerical and underscore shortnames" do
      tests = Array.new
      tests << "lol wat"
      tests << "lol.wat"
      tests << "lol wat"
      tests << "lol\nwat"
      tests << "--fsdfsdrwerw"
      tests << "fdsfsd "

      tests.each do |test|
        stream = Stream.make
        post :shortname, :id => stream.to_param, :shortname => "lol wat"

        assert_response :redirect
        assert_not_nil flash[:error]
      end
    end

    should "not accept shortnames that are already assigned to another stream" do
      stream = Stream.make(:shortname => "foo")
      stream2 = Stream.make
      post :shortname, :id => stream2.to_param, :shortname => "foo"

      assert_response :redirect
      assert_not_nil flash[:error]
    end

    should "complain about missing shortname" do
      stream = Stream.make()
      post :shortname, :id => stream.to_param

      assert_response :redirect
      assert_not_nil flash[:error]
    end

    should "complain about too long shortname" do
      stream = Stream.make()
      post :shortname, :id => stream.to_param, :shortname => "fooooooooooooooooooobaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaarrrrrrrrrrrrrr"

      assert_response :redirect
      assert_not_nil flash[:error]
    end

  end

  context "related streams" do

    should "set related streams matcher" do
      m = 'foo\d'
      stream = Stream.make
      post :related, :id => stream.to_param, :related_streams_matcher => m

      assert_equal m, assigns(:stream).related_streams_matcher
      assert_response :redirect
      assert_not_nil flash[:notice]
    end

    should "change existing related streams matcher" do
      m = '^foo'
      stream = Stream.make(:related_streams_matcher => "zomg")
      post :related, :id => stream.to_param, :related_streams_matcher => m

      assert_equal m, assigns(:stream).related_streams_matcher
      assert_response :redirect
      assert_not_nil flash[:notice]
    end

    should "not accept empty related streams matcher" do
      stream = Stream.make
      post :related, :id => stream.to_param, :related_streams_matcher => ''

      assert_response :redirect
      assert_not_nil flash[:error]
    end

    should "not accept invalid regex related streams matcher" do
      stream = Stream.make
      post :related, :id => stream.to_param, :related_streams_matcher => '*'

      assert_response :redirect
      assert_not_nil flash[:error]
    end

  end

end
