require 'rubygems'
require 'em-websocket'
require 'mongo'
require 'json'
require 'cgi'
require 'date'

include Mongo

# We need our helpers from Rails to format and convert message parts.
require './app/helpers/application_helper.rb'
include ApplicationHelper

def subscribe_client(socket)
  Thread.new do
    puts "in thread"
    db = Connection.new('localhost',27017,:slave_ok => true).db('graylog2')
    coll = db['messages']
    start_count = coll.count

    loop do
      puts "Creating the cursor"
      cursor = Cursor.new(coll, :tailable => true, :order => [['$natural', 1]]).skip(start_count)
      loop do
        if not cursor.has_next?
          if cursor.closed?
            puts "Cursor closed"
            break
          end
          # This is not guaranteed to sleep 0.2 seconds in most systems, but I just don't care. You'll
          # get longer sleep times in the worst case.
          sleep(0.2)
          next
        end
        doc = cursor.next_document
        
        short_message = CGI.escapeHTML(doc['message'])
        host = CGI.escapeHTML(doc['host'])
        level = syslog_level_to_human(doc['level'])
        facility = syslog_facility_to_human(doc['facility'])
        created_at = gl_date(Time.at(doc['created_at']).to_s)

        socket.send({"short_message" => short_message, "host" => host, "level" => level, "facility" => facility, "created_at" => created_at}.to_json)
      end
    end 
  end
end

EventMachine::WebSocket.start(:host => "0.0.0.0", :port => 12500) do |ws|
  ws.onopen {
    subscribe_client(ws)
  }

  ws.onclose   { puts "WebSocket closed" }
end
