class Initializer
  include Mongoid::Document

  field :name, :type => String
  field :typeclass, :type => String
  field :requested_config, :type => Hash

  STANDARD_INITIALIZERS = %w(
    org.graylog2.initializers.DeflectorThreadsInitializer
    org.graylog2.initializers.AlarmScannerInitializer
    org.graylog2.initializers.IndexRetentionInitializer
    org.graylog2.initializers.AMQPSyncInitializer
    org.graylog2.initializers.DroolsInitializer
    org.graylog2.initializers.ServerValueWriterInitializer
    org.graylog2.initializers.BufferWatermarkInitializer
    org.graylog2.initializers.AnonymousInformationCollectorInitializer
    org.graylog2.initializers.HostCounterCacheWriterInitializer
    org.graylog2.initializers.MessageCounterInitializer
    org.graylog2.initializers.LibratoMetricsInitializer
  )

  def self.all_non_standard
  	all.reject { |o| STANDARD_INITIALIZERS.include?(o.typeclass) }
  end

end