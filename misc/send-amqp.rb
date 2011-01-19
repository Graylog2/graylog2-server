require 'rubygems'
require 'bunny'
require 'json'

#log_msg = {
#  "host" => "somehost",
#  "short_message" => "test message from send-amqp.rb",
#  "version" => "1.0",
#  "timestamp" => Time.now.to_i
#}.to_json

log_msg = "<66> a test syslog log message from send-amqp.rb!"

b = Bunny.new(
  :host => "localhost",
  :port => 5672,
  :user => "guest",
  :pass => "guest",
  :vhost => "/"
)

b.start

queue = b.queue('somequeue3', :durable => true)

#loop do
queue.publish(log_msg)
#end

b.stop
