require 'test_helper'

class MessageGatewayTest < ActiveSupport::TestCase

  context "deleting" do

    should "delete a message" do
      message = bm()
      id = message["_id"]

      assert MessageGateway.delete_message(id)
    end

    should "complain about non existent message" do
      assert !MessageGateway.delete_message("LOLWUT-DONTEXIST")
    end

  end

  context "analyzing" do
    
    should "correctly analyze a text" do
      # !! this will FAIL if a wrong analyzer was provided in mapping
      assert_equal ["LOLWUT", "zomg.wat,", "ohai"], MessageGateway.analyze("LOLWUT zomg.wat, ohai", "message")
    end

    should "not fail on empty text" do
      assert_equal Array.new, MessageGateway.analyze("", "message")
    end

  end

  context "mapping" do

    setup do
      @invalid_template_mapping = {"dynamic_templates"=>[{"store_generic"=>{"mapping"=>{"index"=>"analyzed"}, "match"=>"none"}}], "properties"=>{"_http_verb"=>{"index"=>"not_analyzed", "type"=>"string"}, "_user_id"=>{"index"=>"not_analyzed", "type"=>"string"}, "_http_method"=>{"index"=>"not_analyzed", "type"=>"string"}, "line"=>{"type"=>"long"}, "_started_at"=>{"index"=>"not_analyzed", "type"=>"string"}, "_error_message"=>{"index"=>"not_analyzed", "type"=>"string"}, "level"=>{"type"=>"long"}, "facility"=>{"index"=>"not_analyzed", "type"=>"string"}, "_processed"=>{"index"=>"not_analyzed", "type"=>"string"}, "file"=>{"index"=>"not_analyzed", "type"=>"string"}, "created_at"=>{"type"=>"double"}, "_error_name"=>{"index"=>"not_analyzed", "type"=>"string"}, "_xing_type"=>{"index"=>"not_analyzed", "type"=>"string"}, "host"=>{"index"=>"not_analyzed", "type"=>"string"}, "_request_id"=>{"index"=>"not_analyzed", "type"=>"string"}, "_xing_engine"=>{"index"=>"not_analyzed", "type"=>"string"}, "_oauth_consumer_key"=>{"index"=>"not_analyzed", "type"=>"string"}, "full_message"=>{"analyzer"=>"whitespace", "type"=>"string"}, "_total_time_ms"=>{"index"=>"not_analyzed", "type"=>"string"}, "_total_time"=>{"index"=>"not_analyzed", "type"=>"string"}, "message"=>{"analyzer"=>"whitespace", "type"=>"string"}, "_ip_address"=>{"index"=>"not_analyzed", "type"=>"string"}, "_oauth_access_token"=>{"index"=>"not_analyzed", "type"=>"string"}, "_http_response_code"=>{"type"=>"long"}, "_url"=>{"index"=>"not_analyzed", "type"=>"string"}, "streams"=>{"index"=>"not_analyzed", "type"=>"string"}}}

      @invalid_properties_mapping = {"dynamic_templates"=>[{"store_generic"=>{"mapping"=>{"index"=>"not_analyzed"}, "match"=>"*"}}], "properties"=>{"_http_verb"=>{"index"=>"not_analyzed", "type"=>"string"}, "_user_id"=>{"index"=>"not_analyzed", "type"=>"string"}, "_http_method"=>{"index"=>"not_analyzed", "type"=>"string"}, "line"=>{"type"=>"long"}, "_started_at"=>{"index"=>"not_analyzed", "type"=>"string"}, "_error_message"=>{"index"=>"not_analyzed", "type"=>"string"}, "level"=>{"type"=>"long"}, "facility"=>{"index"=>"not_analyzed", "type"=>"string"}, "_processed"=>{"index"=>"not_analyzed", "type"=>"string"}, "file"=>{"index"=>"not_analyzed", "type"=>"string"}, "created_at"=>{"type"=>"double"}, "_error_name"=>{"index"=>"not_analyzed", "type"=>"string"}, "_xing_type"=>{"index"=>"not_analyzed", "type"=>"string"}, "host"=>{"index"=>"not_analyzed", "type"=>"string"}, "_request_id"=>{"index"=>"not_analyzed", "type"=>"string"}, "_xing_engine"=>{"index"=>"not_analyzed", "type"=>"string"}, "_oauth_consumer_key"=>{"index"=>"not_analyzed", "type"=>"string"}, "full_message"=>{"analyzer"=>"EVIL WRONG BUG", "type"=>"string"}, "_total_time_ms"=>{"index"=>"not_analyzed", "type"=>"string"}, "_total_time"=>{"index"=>"not_analyzed", "type"=>"string"}, "message"=>{"analyzer"=>"whitespace", "type"=>"string"}, "_ip_address"=>{"index"=>"not_analyzed", "type"=>"string"}, "_oauth_access_token"=>{"index"=>"not_analyzed", "type"=>"string"}, "_http_response_code"=>{"type"=>"long"}, "_url"=>{"index"=>"not_analyzed", "type"=>"string"}, "streams"=>{"index"=>"not_analyzed", "type"=>"string"}}}
    end

    should "detect valid mappings" do
      assert MessageGateway.mapping_valid?
    end

    should "detect invalid template mappings" do
      MessageGateway.expects(:message_mapping).returns(@invalid_template_mapping)
      assert !MessageGateway.mapping_valid?
    end
    
    should "detect invalid properties mappings" do
      MessageGateway.expects(:message_mapping).returns(@invalid_properties_mapping)
      assert !MessageGateway.mapping_valid?
    end

  end

end
