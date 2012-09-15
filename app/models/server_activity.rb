class ServerActivity
  include Mongoid::Document

  field :timestamp, :type => Integer
  field :content, :type => String
  field :caller, :type => String

end
