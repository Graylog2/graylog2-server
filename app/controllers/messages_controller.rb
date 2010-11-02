class MessagesController < ApplicationController
  def index
    if params[:filters].blank?
      @messages = Message.all_with_blacklist params[:page]
    else
      @messages = Message.all_by_quickfilter params[:filters], params[:page]
    end
    @total_count =  Message.count_since(0)
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

  def deletebyquickfilter
    begin
      filters = JSON.parse(params[:filters])

      filters_with_symbols = Hash.new
      filters_with_symbols[:message] = filters["message"]
      filters_with_symbols[:facility] = filters["facility"]
      filters_with_symbols[:severity] = filters["severity"]
      filters_with_symbols[:host] = filters["host"]

      conditions = Message.all_by_quickfilter(filters_with_symbols, 0, 0, true)
      throw "Missing conditions" if conditions.blank?

      Message.set(conditions, :deleted => true )

      flash[:notice] = "Messages have been deleted."
    rescue
      flash[:error] = "Could not delete messages."
    end

    redirect_to :action => 'index'
  end

  def deletebystream
    begin
      conditions = Message.all_of_stream params[:id].to_i, 0, true
      throw "Missing conditions" if conditions.blank?

      Message.set(conditions, :deleted => true )

      flash[:notice] = "Messages have been deleted."
    rescue
      flash[:error] = "Could not delete messages."
    end

    redirect_to :controller => 'streams', :action => 'show', :id => params[:id]
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
    conditions[:id] = Hash.new
    conditions[:id]['$nin'] = [message_id]

    # Get the messages.
    @messages = Message.all :limit => 50, :order => "$natural DESC", :conditions => conditions

    if @messages.blank?
      render :text => "No similar messages found"
      return
    end

    @inline_version = true
    render :partial => "table"
  end
end
