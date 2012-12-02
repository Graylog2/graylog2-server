class MessageInput
  include Mongoid::Document

  field :name, :type => String
  field :typeclass, :type => String
  field :requested_config, :type => Hash

  STANDARD_INPUTS = %w(
    org.graylog2.inputs.syslog.SyslogTCPInput
    org.graylog2.inputs.syslog.SyslogUDPInput
    org.graylog2.inputs.gelf.GELFUDPInput
    org.graylog2.inputs.gelf.GELFTCPInput
    org.graylog2.inputs.amqp.AMQPInput
  )

  def self.all_non_standard
  	all.reject { |o| STANDARD_INPUTS.include?(o.typeclass) }
  end

end