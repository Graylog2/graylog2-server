class Cluster
  include Mongoid::Document

  self.collection_name = 'server_values'

  field :type, :type => String
  field :value, :type => Object

  PING_TIMEOUT = 15

  def self.throughput
    return 0 if no_active_nodes?
    active_nodes.map { |n| n.current_throughput }.sum
  end

  def self.highest_throughput
    return 0 if no_active_nodes?
    active_nodes.map { |n| n.highest_throughput }.sum
  end

  def self.no_active_nodes?
    active_nodes.blank?
  end

  def self.active_nodes
    all(:conditions => {:type => "ping", :value => { "$gte" => PING_TIMEOUT.seconds.ago.to_i } }).map { |s| ServerNode.new(s.server_id) }
  rescue
    []
  end

end
