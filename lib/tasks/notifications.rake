namespace :subscriptions do

  desc "Alert all users who subscribed to a stream if it contains new messages"
  task :send => :environment do
    # Go through every stream that has subscribers.
    Stream.all_with_subscribers.each do |stream_id|
      stream = Stream.find(stream_id)

      if stream.last_subscription_check.blank?
        puts "Stream >#{stream.title}< has subscribers but was never checked before. Setting first check date now and skipping."
        stream.last_subscription_check = Time.now
        stream.save

        next
      end

      puts "Stream >#{stream.title}< has subscribers. Checking for new messages since #{stream.last_subscription_check}"
      
      # Are there new messages?
      messages = Message.all_of_stream_since(stream_id, stream.last_subscription_check)
      count = messages.count
      if count > 0
        # Get all subscribers.
        subscribers = SubscribedStream.all_subscribers(stream_id)
        puts "\t#{count} new messages. Sending notifications to #{subscribers.count} subscribers."
        
        # Send messages.
        subscribers.each do |subscriber|
          begin
            Pony.mail(
              :to => subscriber,
              :from => Configuration.subscription_from_address,
              :subject => Configuration.subscription_subject,
              :body => "OHAI",
              :via => Configuration.email_transport_type,
              :smtp => Configuration.email_smtp_settings # Only used when :via => :smtp
            )
            puts "\t[->] #{subscriber}"
          rescue => e
            puts "\t [!!] #{subscriber} (#{e.to_s.delete("\n")})"
          end
        end
      else
        puts "\tNo new messages."
      end
        
      stream.last_subscription_check = Time.now
      stream.save
    end

    puts "All done."
  end
end
