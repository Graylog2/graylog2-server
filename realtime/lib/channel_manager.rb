class ChannelManager

  def initialize
    @channels = {
      :overall => EM::Channel.new,
      :streams => []
    }
  end

  def send_fakes

    loop do
      @channels[:overall].push("OHAI")
      sleep 1
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
    @channels[:streams].each { |c| c.unsubscribe(sid) }
  end

  private
  def determine_channel_target(ws)
    :overall
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

end
