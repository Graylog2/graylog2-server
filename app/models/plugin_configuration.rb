class PluginConfiguration

  include Mongoid::Document

  field :typeclass, :type => String
  field :configuration, :type => Object

end