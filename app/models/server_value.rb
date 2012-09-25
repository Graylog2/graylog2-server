class ServerValue

  include Mongoid::Document

  field :type, :type => String
  field :value, :type => Object
  
  def self.get(key, server_id, if_not_found = "unknown", field = :value)
    val = self.first(:conditions => { "server_id" => server_id, "type" => key })
    val.nil? ? if_not_found : val.__send__(field)
  end

  def self.delete_outdated
    delete_all(:server_id => { "$in" => outdated_nodes })
  end

  private
  def self.outdated_nodes
    all(:conditions => {:type => "ping", :value => { "$lt" => Cluster::PING_TIMEOUT.seconds.ago.to_i } }).collect { |s| s.server_id }
  rescue
    []
  end

end
