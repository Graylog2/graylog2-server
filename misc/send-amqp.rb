require 'rubygems'
require 'bunny'
require 'json'

log_msg = {
  "host" => "somehost",
  "short_message" => "test message from send-amqp.rb",
}.to_json

#log_msg = "<66> a test syslog log message from send-amqp.rb!"

b = Bunny.new(
  :host => "localhost",
  :port => 5672,
  :user => "guest",
  :pass => "guest",
  :vhost => "/"
)

b.start

e = b.exchange("logmessages-gelf", :type => :topic, :durable => true)

loop do
e.publish(log_msg, :key => "foo")
end

b.stop
