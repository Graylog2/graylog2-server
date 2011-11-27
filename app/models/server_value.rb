class ServerValue
  include Mongoid::Document

  MESSAGE_QUEUE_UNLIMITED_SIZE = -1

  field :type, :type => String

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

  def self.message_queue_maximum_size_unlimited?
    MESSAGE_QUEUE_UNLIMITED_SIZE == message_queue_maximum_size
  end

  def self.message_queue_maximum_size_human
    message_queue_maximum_size_unlimited? ? "NO" : message_queue_maximum_size
  end

  def self.message_queue_maximum_size
    get("message_queue_maximum_size")
  end

  def self.message_queue_batch_size
    get("message_queue_batch_size")
  end

  def self.message_queue_poll_freq
    get("message_queue_poll_freq")
  end

  def self.message_queue_current_size
    get("message_queue_current_size")
  end

  def self.message_retention_last_performed
    get("message_retention_last_performed", nil)
  end

  private
  def self.get(key, if_not_found = "unknown")
    val = self.first(:conditions => { "type" => key })

    val.blank? ? if_not_found : val.value
  end

end
