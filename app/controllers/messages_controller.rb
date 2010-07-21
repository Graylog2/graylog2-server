class MessagesController < ApplicationController
  def index
    if params[:filters].blank?
      @messages = Message.all_with_blacklist params[:page]
    else
      @messages = Message.all_by_quickfilter params[:filters], params[:page]
    end
    @total_count =  Message.count
    @total_blacklisted_terms = BlacklistedTerm.count

    @favorites = FavoritedStream.find_all_by_user_id current_user.id
  end

  def show
    @message = Message.find params[:id]
  end

  def getcompletemessage
    message = Message.find params[:id]
    render :text => CGI.escapeHTML(message.message)
  end
end