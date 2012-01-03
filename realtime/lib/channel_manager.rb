class InvalidHTTPVerbException < RuntimeError
end
class InvalidPathException < RuntimeError
end

class ChannelManager

  attr_reader :channels

  def initialize
    @channels = {
      :overall => EM::Channel.new,
      :streams => Hash.new
    }
  end

  def route_message(streams, received_at, message)
    date = format_date(Time.at(received_at)) rescue "unknown"
    full_message = "#{date} - #{message}"
    push_to_overall(full_message)
   
    # Possibly push to stream consumers.
    if streams.is_a?(Array) and streams.size > 0
      push_to_streams(streams, full_message)
    end
  end

  def register_client(ws)
    target = determine_channel_target(ws)
    if target == :overall
      type = :overall
      sid = @channels[:overall].subscribe { |msg| push_message(ws, msg) }
    else
      type = :stream
      sid = register_stream_client(ws, target)
    end

    {
      :sid => sid,
      :target => {
        :type => type,
        :name => target
      }
    }
  end

  # unregisters client from all channels.
  def unregister_client(sid)
    @channels[:overall].unsubscribe(sid)
    @channels[:streams].each { |k,v| v.unsubscribe(sid) }
  end

  private
  def determine_channel_target(ws)
    method = ws.request["method"]
    path = ws.request["path"]
    raise(InvalidHTTPVerbException, "Invalid HTTP method <#{method}>") unless method == "GET"
    
    return :overall if path == "/overall"
    if path =~ /^\/stream\//
      r = path.scan(/\/stream\/(.+)\/?/)[0][0]
      r.chop! if r[-1] == "/" # Remove possible trailing slash.
      
      return r
    end

    raise(InvalidPathException, "Invalid path <#{path}>. Only /overall or /stream are supported.")
  end

  def register_stream_client(ws, stream_id)
    if @channels[:streams][stream_id].nil?
      @channels[:streams][stream_id] = EM::Channel.new
    end
    @channels[:streams][stream_id].subscribe { |msg| push_message(ws, msg) }
  end

  # actually sends the message to the client
  def push_message(ws, msg)
    ws.send(msg)
  end
  
  def push_to_overall(message)
    @channels[:overall].push(message)
  end

  def push_to_streams(streams, message)
    streams.each do |stream|
      stream = stream.to_s
      # Just ignore if nobody subscribed to the stream.
      next if @channels[:streams][stream].nil?

      @channels[:streams][stream].push(message)
    end
  end

  def format_date(t)
    "#{t.strftime('%H:%M:%S')}.#{t.usec.to_s[0,3]}" rescue "0"
  end

end
