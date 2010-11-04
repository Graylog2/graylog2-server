namespace :alert do
  desc "Alert users if an alertable stream contains new messages"
  task :send => :environment do
    # get the previous alert timestamp
    newer_than = Alert.last ? Alert.last.created_at : nil
    # start alert body
    body = ""
    # group messages
    streams = Stream.find(:all, :conditions => "alertable = true")
    streams.each do |stream|
      messages = Message.all_of_stream(stream.id, nil, nil, newer_than).group_by {|message| message.host + message.message}.collect do |message, occurrences|
        body += "(" + occurrences.size.to_s + ") Host: '" + occurrences.first.host + "'   Message: '" + occurrences.first.message + "'\n"
        body += "       Occurrences:\n"
        occurrences.each do |occurrence|
          body += "           " + occurrence.date + "\n"
        end
        body += "\n\n"
      end
    end
    # don't send dummy alerts
    unless body == ""
      # save alert body with timestamp
      alert = Alert.new
      alert.body = body
      alert.save
      # send an alert email to every user
      users = User.all
      users.each do |user|
        Pony.mail(:to => user.email, :from => 'alerts@graylog2.org', :subject => 'GrayLog2 Alert!', :body => body)
      end
    end
  end
end
