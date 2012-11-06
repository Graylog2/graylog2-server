class Transport
  include Mongoid::Document

  field :name, :type => String
  field :typeclass, :type => String

end