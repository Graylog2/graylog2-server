class Host
  include Mongoid::Document

  field :host, :type => String
  field :message_count, :type => Float  # FIXME float??? so we can have 3.14 messages from this host?

  validates_presence_of :host

end
