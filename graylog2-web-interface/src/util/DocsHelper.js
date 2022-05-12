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
    AUTHENTICATORS: 'permission-management#authentication',
    CHANGELOG: 'changelog',
    CLUSTER_STATUS_EXPLAINED: 'elasticsearch#cluster-status-explained',
    COLLECTOR: 'sidecar',
    COLLECTOR_SIDECAR: 'sidecar',
    COLLECTOR_STATUS: 'sidecar#sidecar-status',
    CONFIGURING_ES: 'elasticsearch',
    DASHBOARDS: 'dashboards',
    DECORATORS: 'decorators',
    ENTERPRISE_CHANGELOG: 'changelog-graylog',
    ENTERPRISE_SETUP: 'setup',
    ES_CLUSTER_STATUS_RED: 'elasticsearch#cluster-status-explained',
    ES_CLUSTER_UNAVAILABLE: 'elasticsearch#configuration',
    ES_OPEN_FILE_LIMITS: 'elasticsearch#configuration',
    EXTRACTORS: 'extractors',
    INDEXER_FAILURES: 'indexer-failures',
    INDEX_MODEL: 'index-model',
    LOAD_BALANCERS: 'load-balancers',
    LOOKUPTABLES: 'lookuptables',
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

  toString(path) {
    const baseUrl = this.DOCS_URL;

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
