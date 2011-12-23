require 'test_helper'
require 'rails/performance_test_help'

class MessagesIndexTest < ActionDispatch::PerformanceTest
  setup do
    load './test/blueprints.rb'  # HACK

    Message::LIMIT.times { Message.make }
    assert_equal Message::LIMIT, Message.count

    user = User.make
    assert_equal 200, post_via_redirect('/session', :login => user.login, :password => user.password)
  end

  def test_messages_index
    get '/messages'
    assert_select 'tr.message-row', Message::LIMIT
  end
end

puts "Check tmp/performance after run"
