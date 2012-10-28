class AmqpConfiguration

  include Mongoid::Document
  include Mongoid::Timestamps

  field :disabled, :type => Boolean
  field :exchange, :type => String
  field :routing_key, :type => String
  field :input_type, :type => String
  field :ttl, :type => Integer

  validates_presence_of :exchange, :routing_key, :input_type, :ttl
  validates_numericality_of :ttl


  def queue_name
  	return "gl2-#{input_type.downcase}-#{exchange}-#{id}"
  end

end