require './lib/web_socket.rb'

client = WebSocket.new("ws://localhost:9001/")
client.send("Hello")
while data = client.receive()
  puts("got: #{data}")
end
