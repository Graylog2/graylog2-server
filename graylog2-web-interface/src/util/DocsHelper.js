import Version from 'util/Version';

class DocsHelper {
  PAGES = {
    ALERTS: 'streams/alerts.html',
    CLUSTER_STATUS_EXPLAINED: 'configuration/elasticsearch.html#cluster-status-explained',
    COLLECTOR: 'collector.html',
    COLLECTOR_SIDECAR: 'sidecar.html',
    COLLECTOR_STATUS: 'sidecar.html#sidecar-status',
    CONFIGURING_ES: 'configuration/elasticsearch.html',
    DASHBOARDS: 'dashboards.html',
    DECORATORS: 'queries.html#decorators',
    ES_CLUSTER_STATUS_RED: 'configuration/elasticsearch.html#cluster-status-explained',
    ES_CLUSTER_UNAVAILABLE: 'configuration/elasticsearch.html#configuration',
    ES_OPEN_FILE_LIMITS: 'configuration/elasticsearch.html#open-file-limits',
    EXTRACTORS: 'extractors.html',
    INDEXER_FAILURES: 'indexer_failures.html',
    INDEX_MODEL: 'configuration/index_model.html',
    LOAD_BALANCERS: 'configuration/load_balancers.html',
    PAGE_FLEXIBLE_DATE_CONVERTER: 'extractors.html#the-flexible-date-converter',
    PAGE_STANDARD_DATE_CONVERTER: 'extractors.html#the-standard-date-converter',
    PIPELINE_FUNCTIONS: 'pipelines/functions.html',
    PIPELINE_RULES: 'pipelines/rules.html',
    PIPELINES: 'pipelines.html',
    REPORTING: 'reporting.html',
    SEARCH_QUERY_LANGUAGE: 'queries.html',
    STREAMS: 'streams.html',
    STREAM_PROCESSING_RUNTIME_LIMITS: 'streams.html#stream-processing-runtime-limits',
    USERS_ROLES: 'users_and_roles.html',
    WELCOME: '', // Welcome page to the documentation
  };

  DOCS_URL = 'https://docs.graylog.org/en/';

  toString(path) {
    const baseUrl = this.DOCS_URL + Version.getMajorAndMinorVersion();
    return path === '' ? baseUrl : `${baseUrl}/pages/${path}`;
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
