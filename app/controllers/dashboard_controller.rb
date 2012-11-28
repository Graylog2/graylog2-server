class DashboardController < ApplicationController
  filter_resource_access

  layout "dashboard"

  STANDARD_MAX_MESSAGES = 100
  STANDARD_TIMESPAN = 5

  def index
    @stream = Stream.find_by_id(params[:stream_id]) if params[:stream_id]

    if @stream.blank?
      render :text => 'Stream not found.', :code => :not_found
      return
    end

    # Check if (reader) user is allowed to access this stream.
    if !@stream.accessable_for_user?(current_user)
      render :text => 'You are not allowed to access this dashboard.', :code => :forbidden
      return
    end

    @stream_title = @stream.title
    if @stream.alarm_active and !@stream.alarm_limit.blank? and !@stream.alarm_timespan.blank?
      @message_count = @stream.message_count_since(@stream.alarm_timespan.minutes.ago.to_i)
      @timespan = @stream.alarm_timespan
      @max_messages = @stream.alarm_limit
    else
      @message_count = @stream.message_count_since(STANDARD_TIMESPAN.minutes.ago)
      @max_messages = STANDARD_MAX_MESSAGES
      @timespan = STANDARD_TIMESPAN
    end
    @messages = MessageGateway.all_of_stream_paginated(@stream.id)
  end

end
