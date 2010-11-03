namespace :alert do
  desc "Alert users if an alertable stream contains new messages"
  task :send => :environment do
    body = "Messages:\n\n"
    streams = Stream.find(:all, :conditions => "alertable = true")
    streams.each do |stream|
      messages = Message.all_of_stream(stream.id).group_by {|message| message.message}.collect do |message, details|
        body += "(" + details.size.to_s + ") " + message + "\n\n"
      end
    end
    users = User.all
    users.each do |user|
      Pony.mail(:to => user.email, :from => 'alerts@graylog2.org', :subject => 'GrayLog2 Alert!', :body => body)
    end
  end
end
