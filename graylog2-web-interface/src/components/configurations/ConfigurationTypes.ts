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

export { ConfigurationType };
export default ConfigurationType;
