require 'test_helper'

class StreamsControllerTest < ActionController::TestCase

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

end
