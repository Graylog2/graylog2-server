class Forwarder
  include Mongoid::Document

  field :title, :type => String
  field :endpoint_type, :type => String
  field :host, :type => String
  field :port, :type => Integer

  embedded_in :stream, :inverse_of => :forwarders

  # Other validations done when saving because they depend too muc on the endpoint_type.
  validates_presence_of :title
  validates_presence_of :endpoint_type
  validates_numericality_of :port

  def human_endpoint_type
    return "Unknown" if self.endpoint_type.blank?
    case self.endpoint_type.to_sym
      when :syslog then return "UDP Syslog"
      when :gelf then return "GELF"
      when :loggly then return "Logg.ly"
      else return "Unknown"
    end
  end
end
