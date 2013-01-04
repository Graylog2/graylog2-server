class Transport
  include Mongoid::Document

  field :name, :type => String
  field :user_field_name, :type => String
  field :typeclass, :type => String

  STANDARD_TRANSPORTS = %w(
    org.graylog2.alarms.transports.EmailTransport
    org.graylog2.alarms.transports.JabberTransport
  )

  def self.all_non_standard
  	all.reject { |o| STANDARD_TRANSPORTS.include?(o.typeclass) }
  end

end