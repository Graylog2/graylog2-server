import Version from './Version';

class DocsHelper {
  PAGES = {
    ALERTS: 'streams.html#alerts',
    CLUSTER_STATUS_EXPLAINED: 'configuring_es.html#cluster-status-explained',
    COLLECTOR: 'collector.html',
    COLLECTOR_SIDECAR: 'collector_sidecar.html',
    CONFIGURING_ES: 'configuring_es.html',
    DASHBOARDS: 'dashboards.html',
    ES_CLUSTER_STATUS_RED: 'configuring_es.html#cluster-status-explained',
    ES_CLUSTER_UNAVAILABLE: 'configuring_es.html#configuration',
    ES_OPEN_FILE_LIMITS: 'configuring_es.html#open-file-limits',
    EXTERNAL_DASHBOARDS: 'external_dashboards.html',
    EXTRACTORS: 'extractors.html',
    FLEXIBLE_DATE_CONVERTER: 'extractors.html#the-flexible-date-converter',
    INDEXER_FAILURES: 'indexer_failures.html',
    INDEX_MODEL: 'index_model.html',
    LOAD_BALANCERS: 'load_balancers.html',
    PAGE_FLEXIBLE_DATE_CONVERTER: 'extractors.html#the-flexible-date-converter',
    PAGE_STANDARD_DATE_CONVERTER: 'extractors.html#the-standard-date-converter',
    PIPELINE_FUNCTIONS: 'pipelines/functions.html',
    PIPELINE_RULES: 'pipelines/rules.html',
    PIPELINES: 'pipelines.html',
    SEARCH_QUERY_LANGUAGE: 'queries.html',
    STANDARD_DATE_CONVERTER: 'extractors.html#the-standard-date-converter',
    STREAMS: 'streams.html',
    STREAM_PROCESSING_RUNTIME_LIMITS: 'streams.html#stream-processing-runtime-limits',
    USERS_ROLES: 'users_and_roles.html',
    WELCOME: '', // Welcome page to the documentation
  };
  DOCS_URL = 'http://docs.graylog.org/en/';

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
