class ServerNode
  
  attr_reader :server_id

  def initialize(server_id)
    @server_id = server_id
  end

  def is_master?
    ServerValue.get("is_master", @server_id, 0)
  end

  def local_hostname
    ServerValue.get("local_hostname", @server_id)
  end

  def current_throughput
    get_throughput(:current)
  end
  
  def highest_throughput
    get_throughput(:highest)
  end

  def buffer_watermark(buffer)
    get_buffer_watermark(buffer, :number)
  end

  def buffer_watermark_percentage(buffer)
    get_buffer_watermark(buffer, :percentage)
  end

  def startup_time
    ServerValue.get("startup_time", @server_id, 0)
  end

  def available_processors
    ServerValue.get("available_processors", @server_id)
  end

  def jre
    ServerValue.get("jre", @server_id)
  end
  
  def pid
    ServerValue.get("pid", @server_id)
  end
  
  def graylog2_version
    ServerValue.get("graylog2_version", @server_id)
  end

  def message_retention_last_performed
    v = ServerValue.get("message_retention_last_performed", @server_id, nil)

    v.blank? ? nil : v.value
  end

  private
  def get_throughput(which)
    ServerValue.get("total_throughput", @server_id, -1, which)
  rescue
    -1
  end

  def get_buffer_watermark(buffer, type)
    case type
      when :number
        key = buffer
      when :percentage
        key = buffer + "_percent"
    end

    ServerValue.get("buffer_watermarks", @server_id, -1, key)
  rescue
    -1
  end

end
