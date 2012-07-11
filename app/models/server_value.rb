class ServerValue

  include Mongoid::Document

  field :type, :type => String
  field :value, :type => Object
  
  def self.get(key, server_id, if_not_found = "unknown")
    val = self.first(:conditions => { "server_id" => server_id, "type" => key })
    val.blank? ? if_not_found : val
  end

end
