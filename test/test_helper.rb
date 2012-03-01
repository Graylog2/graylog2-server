ENV["RAILS_ENV"] = "test"
require File.expand_path('../../config/environment', __FILE__)
require 'rails/test_help'

require 'blueprints'

# Must be equal to the one graylog2-server generates.
#  - Generated via: System.out.println(Mapping.get()); - MAKE SURE TO HAVE HASH START WITH TYPE (message)
ES_MESSAGE_MAPPING = JSON.parse('{"message":{"dynamic_templates":[{"store_generic":{"mapping":{"index":"not_analyzed"}, "match":"*"}}], "properties":{"message":{"index":"analyzed", "analyzer":"whitespace", "type":"string"}, "full_message":{"index":"analyzed", "analyzer":"whitespace", "type":"string"}, "created_at":{"type":"double"}}}}')
ES_TEST_INDEX_NAME = "graylog2_test"

class ActiveSupport::TestCase
  setup do
    Timecop.return

    DatabaseCleaner.clean

    # Remove and re-create ElasticSearch index
    Tire.index(ES_TEST_INDEX_NAME) do
      delete
      create(:mappings => ES_MESSAGE_MAPPING)
    end

    Sham.reset

    Rails.cache.clear
    FilteredTerm.expire_cache
  end

  def build_message(message = {})
    raise "You cannot overwrite type attribute." if !message[:type].blank? or !message["type"].blank?

    standard_message = {
      :type => MessageGateway::TYPE_NAME,
      :message => Faker::Lorem.words(100).join,
      :facility => Faker::Lorem.words(50).join,
      :file => Faker::Lorem.words(50).join('/'),
      :line => rand(8),
      :level => rand(7) + 1,
      :host => Faker::Lorem.words(3).join('-'),
      :created_at => Time.now.to_f,
      :deleted => false
    }
    complete_message = standard_message.merge(message)

    i = Tire.index(ES_TEST_INDEX_NAME)
    stored_message = i.store(complete_message)
    i.refresh

    return stored_message
  end

  # shortcut
  def bm(message = {})
    build_message(message)
  end
end

class ActionController::TestCase
  setup do
    login!
  end

  def login!(options = {})
    user = User.make(options)
    @request.session[:user_id] = user._id
    @logged_in_user = user
    user
  end
end
