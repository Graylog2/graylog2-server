require 'em-websocket'
require 'json'
require File.expand_path('../lib/channel_manager.rb', __FILE__)

# auth token
# configurable

class WebsocketServer

  def log(what)
    puts "#{Time.now} - #{what}"
  end

  def initialize
    @channel_manager = ChannelManager.new
  end

  def start
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
  end

end
