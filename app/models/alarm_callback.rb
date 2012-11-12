class AlarmCallback
  include Mongoid::Document

  field :name, :type => String
  field :typeclass, :type => String
  field :requested_config, :type => Hash

end