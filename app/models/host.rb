class Host
  include Mongoid::Document

  PER_PAGE = 30
  paginates_per(PER_PAGE)

  field :host, :type => String
  field :message_count, :type => Float  # FIXME float??? so we can have 3.14 messages from this host?

  validates_presence_of :host

end
