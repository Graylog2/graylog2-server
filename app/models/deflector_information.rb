class DeflectorInformation
  include Mongoid::Document

  field :server_id, :type => String
  field :deflector_target, :type => String
  field :max_messages_per_index, :type => Integer
  field :timestamp, :type => Integer

end