class MessagesController < ApplicationController
  def index
    @messages = Message.all_with_blacklist params[:page]
    @total_count =  Message.count
    @total_blacklisted_terms = BlacklistedTerm.count

    @favorites = FavoritedStream.find_all_by_user_id current_user.id
  end
end