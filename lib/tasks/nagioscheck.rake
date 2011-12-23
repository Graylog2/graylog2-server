namespace :nagios do
  desc "Check if the number of new log messages in the last X minutes is too high. Usage: "

  task :check => :environment do |task, args|
    # Check if all parameters are set.
    if ENV['minutes'].blank? or ENV['messages'].blank?
      puts "status: error (missing parameters)"
      exit 3
    end

    minutes = ENV['minutes'].to_i
    max_messages = ENV['messages'].to_i

    # Get message count of defined last minutes and compare to given maximum.
    #message_count = Message.total_count_of_last_minutes(minutes)
    message_count =  MessageGateway.all_in_range(nil, minutes.minutes.ago.to_i, Time.now.to_i).total_result_count
    if message_count > max_messages
      puts "status: alert"
      exit 2
    else
      puts "status: okay"
      exit 0
    end

  end

end
