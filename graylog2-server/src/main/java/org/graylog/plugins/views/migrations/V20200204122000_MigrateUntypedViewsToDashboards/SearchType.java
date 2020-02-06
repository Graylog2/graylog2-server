package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;

import java.util.Set;

class SearchType {
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_STREAMS = "streams";
    private final Document searchTypeDocument;

    SearchType(Document searchTypeDocument) {
        this.searchTypeDocument = searchTypeDocument;
    }

    public void syncWithWidget(Widget widget) {
        this.searchTypeDocument.remove(FIELD_FILTER);
        widget.query().ifPresent(this::setQuery);
        widget.timerange().ifPresent(this::setTimerange);
        setStreams(widget.streams());
    }

    private void setQuery(Document query) {
        this.searchTypeDocument.put(FIELD_QUERY, query);
    }

    private void setTimerange(Document timerange) {
        this.searchTypeDocument.put(FIELD_TIMERANGE, timerange);
    }

    private void setStreams(Set<String> streams) {
        this.searchTypeDocument.put(FIELD_STREAMS, streams);
    }
}
