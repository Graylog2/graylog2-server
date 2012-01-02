require File.expand_path('../../../realtime/websocket_server.rb', __FILE__)

namespace :realtime do
  namespace :websocket do

    desc "Starts the websocket server"
    task :start_server do
      s = WebsocketServer.new("127.0.0.1", 9001)
      s.start
    end

  end
end
