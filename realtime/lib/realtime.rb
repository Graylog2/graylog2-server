require 'mongo'

class Realtime

  DB = "graylog2"
  COLLECTION = "realtime_messages"

  def self.subscribe
    db = Mongo::Connection.new().db(DB)
    coll = db.collection(COLLECTION)

    cursor = Mongo::Cursor.new(coll, :tailable => true)

    loop do
      if message = cursor.next_document
        yield(message)
      else
        sleep 0.2
      end
    end

  end

end
