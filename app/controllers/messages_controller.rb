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

  def getsimilarmessages
    # Get the message we want to compare.
    message = Message.find params[:id]
    message_id = message.id
    message = message.message

    ## Prepare comparison
    # Cut until first ':' if there is one and it is in the first half of the message (this is usually the hostname)
    if message.include?(':') and message.index(':') <= message.length/2
      message = message[message.index(':')+1..message.length].strip
    end

    # Get first 30% chars of message. This is what we will do a REGEX search over all messages.
    get_chars = ((message.length.to_f)/100*30).to_i
    message = message[0..get_chars]

    ## Get the messages
    # Blacklist
    conditions = Hash.new
    (blacklist = BlacklistedTerm.get_all_as_condition_hash).blank? ? nil : conditions[:message] = blacklist;

    # Add our search condition.
    conditions[:message] = Hash.new if conditions[:message].blank?
    conditions[:message]['$in'] = [/#{Regexp.escape(message)}/]

    # Don't search for the message we used to compare.
    #conditions[:id] = Hash.new
    #conditions[:id]['$nin'] = [message_id]

    # Get the messages.
    @messages = Message.all :limit => 50, :order => "_id DESC", :conditions => conditions

    if @messages.blank?
      render :text => "No similar messages found"
      return
    end

    @inline_version = true
    render :partial => "table"
  end
end