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

end
