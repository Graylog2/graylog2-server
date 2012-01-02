require 'em-websocket'
require 'json'
require File.expand_path('../lib/channel_manager.rb', __FILE__)
require File.expand_path('../lib/realtime.rb', __FILE__)

# auth token

class WebsocketServer

  attr_reader :listen_host, :listen_port

  def log(what)
    puts "#{Time.now} - #{what}"
  end

  def initialize(listen_host, listen_port)
    @listen_host = listen_host
    @listen_port = listen_port

    @channel_manager = ChannelManager.new
  end

  def start
    Thread.new do
      Realtime.subscribe do |message|
        @channel_manager.route_message(message["streams"], message["message"])
      end
    end

    EventMachine.run {
      
      EventMachine::WebSocket.start(:host => @listen_host, :port => @listen_port) do |ws|

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
