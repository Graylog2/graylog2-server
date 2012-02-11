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
      messages = MessageGateway.all_in_range(1, stream.last_alarm_check, Time.now.to_i, :stream_id => stream.id)
      count = messages.total_result_count
      if count > stream.alarm_limit
        subscribers = AlertedStream.all_subscribers(stream)
        puts "\t#{count} messages: Above limit! Sending alarm to #{subscribers.count} subscribed users."

        # Build email body.
        body = "# Stream >#{stream.title}< had #{count} new messages in the last #{stream.alarm_timespan} minutes. Limit: #{stream.alarm_limit}\n"

        # Add description to body.
        if stream.description.blank?
          body += "# No stream description set.\n\n"
        else
          body += "# Description: #{stream.description}\n\n"
        end

        # Add a few messages for context
        body += "Last messages:\n"
        messages.each do |msg|
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
