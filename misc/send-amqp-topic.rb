require 'rubygems'
require 'bunny'
require 'json'

exchange_opts = {
  :name => 'graylog2',
  :durable  => false,
  :auto_delete => false,
  :type  => :topic
}

b = Bunny.new(
  :host => "localhost",
  :port => 5672,
  :user => "guest",
  :pass => "guest",
  :vhost => "/"
)

b.start

exchange = b.exchange(exchange_opts[:name], exchange_opts)

msg = {
  "host" => "somehost",
  "version" => "1.0",
}

10.times do |n|
  msg["timestamp"] = Time.now.to_i
  msg["short_message"] = "message number #{n}"
  gelf = msg.to_json

  if (n % 2 == 0)
    routing_key = 'number.even'
  else
    routing_key = 'number.odd'
  end

  exchange.publish(gelf, :key => routing_key)
  sleep(0.5)
end

b.stop
