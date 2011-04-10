class ServerValue
  include Mongoid::Document

  key :type, String

  def self.throughput
    val = self.first(:conditions => { "type" => "total_throughput" })
    if val.blank?
      { :current => 0, :highest => 0 }
    else
      { :current => val.current.to_i, :highest => val.highest.to_i }
    end
  end

  def self.graylog2_version
    get("graylog2_version")
  end

  def self.local_hostname
    get("local_hostname")
  end

  def self.pid
    get("pid")
  end

  def self.jre
    get("jre")
  end

  def self.available_processors
    get("available_processors")
  end

  def self.startup_time
    get("startup_time")
  end

  def self.ping
    ping = get("ping")
    ping == "unknown" ? Time.at(0) : Time.at(ping)
  end

  private
  def self.get(key)
    val = self.first(:conditions => { "type" => key })

    val.blank? ? "unknown" : val.value
  end

end
