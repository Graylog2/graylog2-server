class ServerActivity
  include Mongoid::Document

  field :timestamp, :type => Integer
  field :content, :type => String
  field :caller, :type => String
  field :node_id, :type => String
end
