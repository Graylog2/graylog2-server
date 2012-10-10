class Cluster
  include Mongoid::Document

  self.collection_name = 'server_values'

  field :type, :type => String
  field :value, :type => Object

  PING_TIMEOUT = 7

  def self.throughput
    return 0 if no_active_nodes?
    (active_nodes.map { |n| n.current_throughput }.sum/5).to_i
  end

  def self.highest_throughput
    return 0 if no_active_nodes?
    (active_nodes.map { |n| n.highest_throughput }.sum/5).to_i
  end

  def self.no_active_nodes?
    active_nodes.blank?
  end

  def self.active_nodes
    all(:conditions => {:type => "ping", :value => { "$gte" => PING_TIMEOUT.seconds.ago.to_i } }).map { |s| ServerNode.new(s.server_id) }
  rescue
    []
  end

  def self.multiple_masters?
    master_count > 1
  end

  def self.no_masters?
    master_count == 0
  end

  def self.message_retention_last_performed
    # Just in case somebody runs multiple masters, select the most recent run.
    Cluster.active_nodes.collect { |n| n.message_retention_last_performed }.delete_if { |t| t.nil? }.max
  end

  private
  def self.master_count
    active_nodes.count { |n| n.is_master? }
  end

end
