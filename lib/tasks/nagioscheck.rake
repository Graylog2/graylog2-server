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
    message_count = Message.count_of_last_minutes minutes
    if message_count > max_messages
      puts "status: alert"
      exit 2
    else
      puts "status: okay"
      exit 0
    end

  end

end