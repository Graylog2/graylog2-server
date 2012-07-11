class ServerNode
  
  attr_reader :server_id

  def initialize(server_id)
    @server_id = server_id
  end

  def is_master?
    ServerValue.get("is_master", @server_id, 0).value == true
  end

  def current_throughput
    get_throughput(:current)
  end
  
  def highest_throughput
    get_throughput(:highest)
  end

  def startup_time
    ServerValue.get("startup_time", @server_id, 0).value
  end

  def available_processors
    ServerValue.get("available_processors", @server_id).value
  end

  def jre
    ServerValue.get("jre", @server_id).value
  end
  
  def pid
    ServerValue.get("pid", @server_id).value
  end
  
  def graylog2_version
    ServerValue.get("graylog2_version", @server_id).value
  end

  private
  def get_throughput(which)
    val = ServerValue.get("total_throughput", @server_id)
    case which
      when :current then return val.__send__(:current)
      when :highest then return val.__send__(:highest)
    end
  rescue
    -1
  end

end
