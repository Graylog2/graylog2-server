class AlarmCallback
  include Mongoid::Document

  field :name, :type => String
  field :typeclass, :type => String

end