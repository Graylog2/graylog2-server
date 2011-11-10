class DashboardController < ApplicationController
  filter_resource_access

  layout "dashboard"

  STANDARD_MAX_MESSAGES = 100
  STANDARD_TIMESPAN = 5

  def index
    if params[:stream_id].blank?
      @timespan = Setting.get_message_count_interval(current_user)
      @message_count = MessageCount.total_count_of_last_minutes(@timespan)
      @max_messages = Setting.get_message_max_count(current_user)
      @messages = MessageGateway.all_paginated
    else
      stream = Stream.find_by_id(params[:stream_id])
      @stream_title = stream.title
      if stream.alarm_active and !stream.alarm_limit.blank? and !stream.alarm_timespan.blank?
        @message_count = stream.message_count_since(stream.alarm_timespan.minutes.ago.to_i)
        @timespan = stream.alarm_timespan
        @max_messages = stream.alarm_limit
      else
        @message_count = stream.message_count_since(STANDARD_TIMESPAN.minutes.ago)
        @max_messages = STANDARD_MAX_MESSAGES
        @timespan = STANDARD_TIMESPAN
      end
      @messages = MessageGateway.all_of_stream_paginated(stream.id)
    end
  end

end
