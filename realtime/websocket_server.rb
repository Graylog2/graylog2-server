require 'em-websocket'
require 'json'
require './lib/channel_manager.rb'

# auth token
# configurable

def log(what)
  puts "#{Time.now} - #{what}"
end

def subscribe(msg)
  puts msg.inspect
end

@channel_manager = ChannelManager.new

Thread.new do
  @channel_manager.send_fakes
end

EventMachine.run {
  
  EventMachine::WebSocket.start(:host => "0.0.0.0", :port => 9001) do |ws|

    ws.onopen do
 
      begin
        log("Client connected.")
        c = @channel_manager.register_client(ws)
        sid = c[:sid]
        target = c[:target]
        log("Registered client [#{sid}] to channel <#{c[:target][:type]}:#{c[:target][:name]}>.")
      rescue => e
        log("Error: #{e} \n#{e.backtrace.join("\n")}")
      end

      ws.onclose do
        @channel_manager.unregister_client(sid)
        log("Unregistered client [#{sid}] from all channels.")
      end

    end

  end
  
}
