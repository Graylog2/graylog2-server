class MessageOutput
  include Mongoid::Document

  field :name, :type => String
  field :typeclass, :type => String
  field :requested_config, :type => Hash
  field :requested_stream_config, :type => Hash

  def self.all_non_standard
  	all.reject { |o| o.typeclass == "org.graylog2.outputs.ElasticSearchOutput" }
  end

end