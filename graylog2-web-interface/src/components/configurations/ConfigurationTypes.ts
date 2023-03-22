enum ConfigurationType {
  SEARCHES_CLUSTER_CONFIG = 'org.graylog2.indexer.searches.SearchesClusterConfig',
  MESSAGE_PROCESSORS_CONFIG = 'org.graylog2.messageprocessors.MessageProcessorsConfig',
  SIDECAR_CONFIG = 'org.graylog.plugins.sidecar.system.SidecarConfiguration',
  EVENTS_CONFIG = 'org.graylog.events.configuration.EventsConfiguration',
  INDEX_SETS_DEFAULTS_CONFIG = 'org.graylog2.configuration.IndexSetsDefaultConfiguration',
  URL_WHITELIST_CONFIG = 'org.graylog2.system.urlwhitelist.UrlWhitelist',
  PERMISSIONS_CONFIG = 'org.graylog2.users.UserAndTeamsConfig',
  USER_CONFIG = 'org.graylog2.users.UserConfiguration',
}

enum PluginConfigurationType {
  COLLECTORS_SYSTEM = 'org.graylog.plugins.collector.system.CollectorSystemConfiguration',
  AWS = 'org.graylog.aws.config.AWSPluginConfiguration',
  THREAT_INTEL = 'org.graylog.plugins.threatintel.ThreatIntelPluginConfiguration',
  FAILURE_PROCESSING = 'org.graylog.plugins.failure.config.EnterpriseFailureHandlingConfiguration',
  TRAFFIC_LIMIT_VIOLATION = 'org.graylog.plugins.license.violations.TrafficLimitViolationSettings',
  GEO_LOCATION = 'org.graylog.plugins.map.config.GeoIpResolverConfig',
}

export { ConfigurationType, PluginConfigurationType };
