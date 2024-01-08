/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
enum ConfigurationType {
  SEARCHES_CLUSTER_CONFIG = 'org.graylog2.indexer.searches.SearchesClusterConfig',
  MESSAGE_PROCESSORS_CONFIG = 'org.graylog2.messageprocessors.MessageProcessorsConfig',
  SIDECAR_CONFIG = 'org.graylog.plugins.sidecar.system.SidecarConfiguration',
  EVENTS_CONFIG = 'org.graylog.events.configuration.EventsConfiguration',
  INDEX_SETS_DEFAULTS_CONFIG = 'org.graylog2.configuration.IndexSetsDefaultConfiguration',
  URL_WHITELIST_CONFIG = 'org.graylog2.system.urlwhitelist.UrlWhitelist',
  PERMISSIONS_CONFIG = 'org.graylog2.users.UserAndTeamsConfig',
  USER_CONFIG = 'org.graylog2.users.UserConfiguration',
  CERTIFICATE_RENEWAL_POLICY_CONFIG = 'org.graylog2.plugin.certificates.RenewalPolicy',
}
export default ConfigurationType;
export { ConfigurationType };
