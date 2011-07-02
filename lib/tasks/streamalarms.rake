namespace :streamalarms do

  desc "Alert all enabled users of a stream if it is above it's alarm limit."
  task :send => :environment do
    # Go through every stream that has enabled alerts.
    Stream.all_with_enabled_alerts.each do |stream_id|
      stream = Stream.find(stream_id)

      # Skip if limit or timespan is not set.
      if stream.alarm_limit.blank? or stream.alarm_timespan.blank?
        puts "Stream >#{stream.title}< has enabled alarms with users but no limit or timepspan set. Skipping."
        next
      end

      check_since = stream.alarm_timespan.minutes.ago
      puts "Stream >#{stream.title}< has enabled alarms. (max #{stream.alarm_limit} msgs/#{stream.alarm_timespan} min) Checking for message count since #{check_since}"

      # Check if above limit.
      count = Stream.message_count_since(stream_id, check_since.to_i)
      if count > stream.alarm_limit
        subscribers = AlertedStream.all_subscribers(stream)
        puts "\t#{count} messages: Above limit! Sending alarm to #{subscribers.count} subscribed users."

        # Build email body.
        body = "# Stream >#{stream.title}< has #{count} new messages in the last #{stream.alarm_timespan} minutes. Limit: #{stream.alarm_limit}\n"

        # TODO: Include the actual path to the stream.
        body += "View stream at #{Configuration.external_hostname}\n"

        # Add description to body.
        if stream.description.blank?
          body += "# No stream description set.\n"
        else
          body += "# Description: #{stream.description}\n"
        end

        # Add a few messages for context
        body += "Last #{Configuration.streamalarm_message_count} messages:\n"
        Message.by_stream(stream_id).order(:id).desc.limit(Configuration.streamalarm_message_count).each do |msg|
          body += "#{Time.at(msg.created_at)} #{msg.facility} #{msg.host} #{msg.level} #{msg.message}"
          body += " #{msg.additional_fields.inspect}" unless msg.additional_fields.empty?
          body += "\n"
        end

        # Send messages.
        subscribers.each do |subscriber|
          begin
            Pony.mail(
              :to => subscriber,
              :from => Configuration.streamalarm_from_address,
              :subject => "#{Configuration.streamalarm_subject} (Stream: #{stream.title})",
              :body => body,
              :via => Configuration.email_transport_type,
              :via_options => Configuration.email_smtp_settings # Only used when :via => :smtp
            )
            puts "\t[->] #{subscriber}"
          rescue => e
            puts "\t [!!] #{subscriber} (#{e.to_s.delete("\n")})"
          end
        end
      else
        puts "\t#{count} messages: Not above limit."
      end

      stream.last_alarm_check = Time.now
      stream.save
    end

    Job.done(AlertedStream::JOB_TITLE)
    puts "All done."
  end
end
