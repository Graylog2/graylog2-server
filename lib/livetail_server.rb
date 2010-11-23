include ApplicationHelper

module LiveTail

  class MongoCursorDispatcher
    include Mongo

    attr_reader :args
    
    def initialize(socket, args = {})
      @socket = socket

      standard_args = {
        :mongo_host => 'localhost',
        :mongo_port => 27017,
        :mongo_database => 'graylog2',
        :mongo_collection => 'messages',
        :mongo_use_auth => false,
        :mongo_user => "",
        :mongo_password => ""
      }

      @args = standard_args.merge(args)

      @db = Connection.new(@args[:mongo_host], @args[:mongo_port], :slave_ok => true).db(@args[:mongo_database])

      # Authenticate to DB if required.
      if @args[:mongo_use_auth] == true
        @db.authenticate(@args[:mongo_user], @args[:mongo_password])
      end

      @coll = @db[@args[:mongo_collection]]
    end

    def run
      loop do
        cursor = Cursor.new(@coll, :tailable => true, :order => [['$natural', 1]]).skip(@coll.count)
        loop do
          unless cursor.has_next?
            # Has the cursor been closed? (Will break and create a new one in the underlying loop)
            if cursor.closed?
              # XXX TODO: log somewhere
              sleep(3)
              break
            end
            
            # This is not guaranteed to sleep 0.2 seconds in most systems, but I just don't care. You'll
            # get longer sleep times in the worst case.
            sleep(0.2)
            next
          end

          # New message!
          transmit(@socket, cursor.next_document)
        end
      end
    end

    def transmit(socket, document)
      payload = {
        "short_message" => CGI.escapeHTML(document['message']),
        "host" => CGI.escapeHTML(document['host']),
        "level" => ApplicationHelper::syslog_level_to_human(document['level']),
        "facility" => syslog_facility_to_human(document['facility']),
        "created_at" => gl_date(Time.at(document['created_at']).to_s)
      }.to_json

      socket.send(payload)
    end
  end

  class Server
    attr_reader :args

    def initialize(args = {})
      standard_args = {
        :port => 12500,
        :mongo_host => 'localhost',
        :mongo_port => 27017,
        :mongo_database => 'graylog2',
        :mongo_collection => 'messages',
        :mongo_use_auth => false,
        :mongo_user => "",
        :mongo_password => ""
      }

      @args = standard_args.merge(args)
    end

    def run
      EventMachine::WebSocket.start(:host => "0.0.0.0", :port => @args[:port]) do |socket|
        socket.onopen do
          # XXX TODO: log somewhere
          @thread = Thread.new do 
            @loop = MongoCursorDispatcher.new(socket, @args)
            @loop.run
          end
        end

        socket.onclose do
          # XXX TODO: log somewhere
          @thread.exit
        end
      end
    end
  end

end
