class ServerNode
  
  attr_reader :server_id

  def initialize(server_id)
    @server_id = server_id
  end

  def current_throughput
    get_throughput(:current)
  end
  
  def highest_throughput
    get_throughput(:highest)
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
