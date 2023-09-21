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

class DocsHelper {
  PAGES = {
    ALERTS: 'alerts',
    AUDIT_LOG: 'auditlog',
    AUTHENTICATORS: 'permission-management#authentication',
    CHANGELOG: 'changelog',
    CLUSTER_STATUS_EXPLAINED: 'elasticsearch#cluster-status-explained',
    COLLECTOR: 'sidecar',
    COLLECTOR_SIDECAR: 'sidecar',
    COLLECTOR_STATUS: 'sidecar#sidecar-status',
    CONFIGURING_ES: 'elasticsearch',
    DASHBOARDS: 'dashboards',
    DECORATORS: 'decorators',
    ENTERPRISE_SETUP: 'setup',
    ES_CLUSTER_STATUS_RED: 'elasticsearch#cluster-status-explained',
    ES_CLUSTER_UNAVAILABLE: 'elasticsearch#configuration',
    ES_OPEN_FILE_LIMITS: 'elasticsearch#configuration',
    EXTRACTORS: 'extractors',
    GRAYLOG_DATA_NODE: 'graylog-data-node',
    INDEXER_FAILURES: 'indexer-failures',
    INDEX_MODEL: 'index-model',
    LICENSE: 'license',
    LOAD_BALANCERS: 'load-balancers',
    LOOKUPTABLES: 'lookuptables',
    OPERATIONS_CHANGELOG: 'changelog-graylog',
    OPEN_SEARCH_SETUP: 'open-search-setup',
    PAGE_FLEXIBLE_DATE_CONVERTER: 'extractors#normalization',
    PAGE_STANDARD_DATE_CONVERTER: 'extractors#normalization',
    PERMISSIONS: 'permission-management',
    PIPELINE_FUNCTIONS: 'functions',
    PIPELINE_RULES: 'rules',
    PIPELINES: 'pipelines',
    REPORTING: 'reporting',
    ROLLING_ES_UPGRADE: 'rolling-es-upgrade',
    SEARCH_QUERY_ERRORS: {
      UNKNOWN_FIELD: 'query-language#unknown-field',
      QUERY_PARSING_ERROR: 'query-language#parse-exception',
      INVALID_OPERATOR: 'query-language#invalid-operator',
      UNDECLARED_PARAMETER: 'query-language#undeclared-parameter',
    },
    SEARCH_QUERY_LANGUAGE: 'query-language',
    STREAMS: 'streams',
    STREAM_PROCESSING_RUNTIME_LIMITS: 'streams#stream-processing-runtime-limits',
    TIME_FRAME_SELECTOR: 'time-frame-selector',
    UPGRADE_GUIDE: 'upgrading-graylog',
    USERS_ROLES: 'permission-management',
    WELCOME: '', // Welcome page to the documentation
  };

  DOCS_URL = 'https://docs.graylog.org/docs';

  NEW_DOCS_URL = 'https://go2docs.graylog.org';

  DOCS_URL_BY_VERSION = {
    '3\\..': 'https://archivedocs.graylog.org/en/3.3',
    '4\\..': `${this.NEW_DOCS_URL}/4-x`,
    '5\\.0': `${this.NEW_DOCS_URL}/5-0`,
    '5\\.1': `${this.NEW_DOCS_URL}/5-1`,
  };

  toString(path, inVersion = null) {
    const currentDocsVersion = Object.keys(this.DOCS_URL_BY_VERSION)[Object.keys(this.DOCS_URL_BY_VERSION).length - 1];
    const version = Object.keys(this.DOCS_URL_BY_VERSION).find((verRex) => {
      const rex = new RegExp(`^${verRex}$`);

      return rex.test(inVersion);
    }) || currentDocsVersion;

    const baseUrl = inVersion ? this.DOCS_URL_BY_VERSION[version] : this.DOCS_URL;

    return path === '' ? baseUrl : `${baseUrl}/${path}`;
  }

  toLink(path, title) {
    return `<a href="${this.toString(path)}" target="_blank">${title}</a>`;
  }

  versionedDocsHomePage() {
    return this.toString('');
  }
}

const docsHelper = new DocsHelper();

export default docsHelper;
