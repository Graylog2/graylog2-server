class MessagesController < ApplicationController
  def index
    @messages = Message.all_with_blacklist
    @total_count =  Message.count
    @total_blacklisted_terms = BlacklistedTerm.count
  end
end