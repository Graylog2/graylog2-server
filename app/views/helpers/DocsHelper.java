package views.helpers;

import org.graylog2.restclient.lib.Version;

public enum DocsHelper {
    PAGE_WELCOME(""), // Welcome page of the documentation
    PAGE_ALERTS("streams.html#alerts"),
    PAGE_CLUSTER_STATUS_EXPLAINED("configuring_es.html#cluster-status-explained"),
    PAGE_COLLECTOR("collector.html"),
    PAGE_CONFIGURING_ES("configuring_es.html"),
    PAGE_DASHBOARDS("dashboards.html"),
    PAGE_ES_OPEN_FILE_LIMITS("configuring_es.html#open-file-limits"),
    PAGE_EXTRACTORS("extractors.html"),
    PAGE_FLEXIBLE_DATE_CONVERTER("extractors.html#the-flexible-date-converter"),
    PAGE_RADIO_ARCHITECTURE("architecture.html#highly-available-setup-with-graylog-radio"),
    PAGE_INDEX_MODEL("index_model.html"),
    PAGE_INDEXER_FAILURES("indexer_failures.html"),
    PAGE_LOAD_BALANCERS("load_balancers.html"),
    PAGE_SEARCH_QUERY_LANGUAGE("queries.html"),
    PAGE_STANDARD_DATE_CONVERTER("extractors.html#the-standard-date-converter"),
    PAGE_STREAM_PROCESSING_RUNTIME_LIMITS("streams.html#stream-processing-runtime-limits"),
    PAGE_STREAMS("streams.html");

    private static final String DOCS_URL = "http://docs.graylog.org/en/";

    private final String path;

    DocsHelper(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        final String version = Version.VERSION.getBranchName();
        final String baseUrl = DOCS_URL + version;

        return path.isEmpty() ? baseUrl : baseUrl + "/pages/" + path;
    }

    public String toLink(String title) {
        return "<a href=\"" + toString() + "\" target=\"_blank\">" + title + "</a>";
    }

}
