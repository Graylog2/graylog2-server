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

const docsHelper = {
  PAGES: {
    ALERTS: 'interacting_with_your_log_data/alerts.html',
    AUDIT_LOG: 'interacting_with_your_log_data/audit_log.html',
    AUTHENTICATORS: 'setting_up_graylog/user_authentication.htm',
    CHANGELOG: 'changelogs/changelog.html',
    CLUSTER_STATUS_EXPLAINED: 'setting_up_graylog/opensearch.htm#ClusterStatusExplained',
    COLLECTOR: 'getting_in_log_data/graylog_sidecar.html',
    COLLECTOR_SIDECAR: 'getting_in_log_data/graylog_sidecar.html',
    COLLECTOR_STATUS: 'getting_in_log_data/getting_started_with_graylog_sidecar.htm#ViewSidecarStatus',
    CONFIGURING_ES: 'setting_up_graylog/opensearch.htm#GraylogConfigurationSettings',
    DASHBOARDS: 'interacting_with_your_log_data/dashboards.html',
    DECORATORS: 'interacting_with_your_log_data/decorators.html',
    ENTERPRISE_SETUP: 'downloading_and_installing_graylog/installing_graylog.html',
    ES_CLUSTER_STATUS_RED: 'setting_up_graylog/opensearch.htm#ClusterStatusExplained',
    ES_CLUSTER_UNAVAILABLE: 'setting_up_graylog/opensearch.htm#GraylogConfigurationSettings',
    ES_OPEN_FILE_LIMITS: 'setting_up_graylog/opensearch.htm#GraylogConfigurationSettings',
    EXTRACTORS: 'making_sense_of_your_log_data/extractors.htm',
    GRAYLOG_DATA_NODE: 'downloading_and_installing_graylog/install_graylog_data_node.htm',
    INDEXER_FAILURES: 'getting_in_log_data/indexer_failures.html',
    INDEX_MODEL: 'setting_up_graylog/index_model.html',
    LICENSE: 'setting_up_graylog/operations_license_management.html',
    LICENSE_MANAGEMENT: 'setting_up_graylog/operations_license_management.html',
    LOAD_BALANCERS: 'setting_up_graylog/load_balancer_integration.html',
    LOOKUPTABLES: 'making_sense_of_your_log_data/lookup_tables.html',
    OPERATIONS_CHANGELOG: 'changelogs/operations_changelog.html',
    OPEN_SEARCH_SETUP: 'setting_up_graylog/opensearch.htm#InstallingOpenSearch',
    PAGE_FLEXIBLE_DATE_CONVERTER: 'making_sense_of_your_log_data/extractors.htm#Normalization',
    PAGE_STANDARD_DATE_CONVERTER: 'making_sense_of_your_log_data/extractors.htm#Normalization',
    PERMISSIONS: 'setting_up_graylog/permission_management.html',
    PIPELINE_FUNCTIONS: 'making_sense_of_your_log_data/functions.html',
    PIPELINE_RULES: 'making_sense_of_your_log_data/pipeline_usage.html',
    PIPELINES: 'making_sense_of_your_log_data/pipelines.html',
    REPORTING: 'interacting_with_your_log_data/reporting.html',
    ROLLING_ES_UPGRADE: 'setting_up_graylog/opensearch.htm#UpgradingtoOpenSearch',
    SEARCH_QUERY_ERRORS: 'making_sense_of_your_log_data/writing_search_queries.html#ErrorTypes',
    SEARCH_QUERY_LANGUAGE: 'making_sense_of_your_log_data/writing_search_queries.html',
    STREAMS: 'making_sense_of_your_log_data/streams.html',
    STREAM_PROCESSING_RUNTIME_LIMITS:
      'making_sense_of_your_log_data/stream_processing.htm#StreamProcessingRuntimeLimits',
    TIME_FRAME_SELECTOR: 'making_sense_of_your_log_data/time_frame_selector.html',
    UPGRADE_GUIDE: 'upgrading_graylog/upgrading_graylog.html',
    USERS_ROLES: 'setting_up_graylog/users_teams.htm',
    WELCOME: '', // Welcome page to the documentation
  },

  DOCS_URL: 'https://go2docs.graylog.org/current',

  toString(path: string) {
    const baseUrl = this.DOCS_URL;

    return path === '' ? baseUrl : `${baseUrl}/${path}`;
  },

  versionedDocsHomePage() {
    return this.toString('');
  },
} as const;

export default docsHelper;
