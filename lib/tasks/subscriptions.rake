namespace :subscriptions do

  desc "Alert all users who subscribed to a stream if it contains new messages"
  task :send => :environment do
    # Go through every stream that has subscribers.
    streams = Stream.all.find_all { |s| s.subscribers.count > 0}
    streams.each do |stream|
      stream = Stream.find(stream.id)

      if stream.last_subscription_check.blank?
        puts "Stream >#{stream.title}< has subscribers but was never checked before. Setting first check date now and skipping."
        stream.last_subscription_check = Time.now
        stream.save

        next
      end

      puts "Stream >#{stream.title}< has subscribers. Checking for new messages since #{Time.at(stream.last_subscription_check)}"

      # Are there new messages?
      messages = Message.all_of_stream_since(stream.id, stream.last_subscription_check)
      count = messages.count
      if count > 0
        # Get all subscribers.
        subscribers = stream.subscribers
        puts "\t#{count} new messages. Sending notifications to #{subscribers.count} subscribers."

        # Build body.
        body = "# Stream >#{stream.title}< has #{count} new messages since #{Time.at(stream.last_subscription_check)}\n\n"
        messages.each do |message|
          body += "#{Time.at(message.created_at)} from >#{message.host}<\n\t#{message.message}\n\n"
        end

        # Send messages.
        subscribers.each do |subscriber|
          begin
            Pony.mail(
              :to => subscriber.email,
              :from => Configuration.subscription_from_address,
              :subject => "#{Configuration.subscription_subject} (Stream: #{stream.title})",
              :body => body,
              :via => Configuration.email_transport_type,
              :via_options => Configuration.email_smtp_settings # Only used when :via => :smtp
            )
            puts "\t[->] #{subscriber.email}"
          rescue => e
            puts "\t [!!] #{subscriber.email} (#{e.to_s.delete("\n")})"
          end
        end
      else
        puts "\tNo new messages."
      end

      stream.last_subscription_check = Time.now
      stream.save
    end

    Job.done("streamsubscription_check")
    puts "All done."
  end
end
