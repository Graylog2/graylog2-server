class ApiController < ApplicationController

  # Get the count of new messages since a given UNIX timestamp
  def get_new_message_count
    since = params[:since].to_i
    
    if since <= 0
      dispatch_error "Missing or invalid parameter: since"
      return
    end
    
    dispatch_success Message.count_since(since)
  end

  private
  def dispatch_success payload
    render :text => { 'status' => 'success', 'payload' => payload }.to_json
  end
  
  def dispatch_error payload
    render :text => { 'status' => 'error', 'payload' => payload }.to_json
  end

end
