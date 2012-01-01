require './lib/web_socket.rb'

target = ARGV[0]

if target.nil? or target.length == 0
  raise "Missing URL. Example call: ruby test_consumer.rb ws://localhost:9001/overall"
end

client = WebSocket.new(target)
client.send("Hello")
while data = client.receive()
  puts("got: #{data}")
end
