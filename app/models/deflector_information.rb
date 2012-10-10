class DeflectorInformation
  include Mongoid::Document

  field :server_id, :type => String
  field :deflector_target, :type => String
  field :max_messages_per_index, :type => Integer
  field :timestamp, :type => Integer

  TIMEOUT = 7

  def self.get_nodes
    self.first['indices'].map { |name,info| info["shards"] }.flatten.collect do |s|
      { :name => s["node_name"], :hostname => s["node_hostname"], :id => s["node_id"] }
    end.uniq
  end

  def self.up_to_date?
    return false if self.first.blank?
    return self.first.timestamp > TIMEOUT.seconds.ago.to_i
  end

end