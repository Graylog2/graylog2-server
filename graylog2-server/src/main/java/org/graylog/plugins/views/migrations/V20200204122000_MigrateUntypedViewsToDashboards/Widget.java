package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;
import org.elasticsearch.common.util.set.Sets;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class Widget {
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_QUERY_STRING = "query_string";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_STREAMS = "streams";

    private final Document widgetDocument;

    Widget(Document widgetDocument) {
        this.widgetDocument = widgetDocument;
    }

    void mergeFilterIntoQueryIfPresent() {
        filter().ifPresent(filter -> {
            widgetDocument.remove(FIELD_FILTER);
            final String newWidgetQuery = concatenateQueryIfExists(filter);
            setQueryString(newWidgetQuery);
        });
    }

    void mergeQuerySpecsIntoWidget(Query query) {
        query.queryString()
                .filter(queryString -> !queryString.trim().isEmpty())
                .ifPresent(this::mergeQueryString);

        if (!timerange().isPresent()) {
            query.timeRange()
                    .ifPresent(this::setTimerange);
        }

        final Set<String> newStreams = Sets.union(query.streams(), this.streams());
        setStreams(newStreams);
    }

    private void mergeQueryString(String queryString) {
        final String newWidgetQuery = concatenateQueryIfExists(queryString);
        setQueryString(newWidgetQuery);
    }

    private void setQueryString(String newWidgetQuery) {
        widgetDocument.put(FIELD_QUERY, createBackendQuery(newWidgetQuery));
    }

    private void setTimerange(Document timerange) {
        widgetDocument.put(FIELD_TIMERANGE, timerange);
    }

    private void setStreams(Set<String> streams) {
        widgetDocument.put(FIELD_STREAMS, streams);
    }

    private Optional<String> filter() {
        return Optional.ofNullable(widgetDocument.getString(FIELD_FILTER));
    }

    private Optional<Document> timerange() { return Optional.ofNullable(widgetDocument.get(FIELD_TIMERANGE, Document.class)); }

    private Set<String> streams() {
        final List<String> rawStreams = widgetDocument.get(FIELD_STREAMS, List.class);
        if (rawStreams == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(rawStreams);
    }

    private String concatenateQueries(String query1, String query2) {
        return query1 + " AND " + query2;
    }

    private String concatenateQueryIfExists(String newQuery) {
        final Optional<String> currentWidgetQuery = extractWidgetQuery(widgetDocument);
        return currentWidgetQuery
                .map(widgetQuery -> concatenateQueries(widgetQuery, newQuery))
                .orElse(newQuery);
    }

    private Optional<String> extractWidgetQuery(Document widget) {
        if (!widget.containsKey(FIELD_QUERY)) {
            return Optional.empty();
        }
        final Document query = (Document)widget.get(FIELD_QUERY);
        if (!query.containsKey(FIELD_QUERY_STRING) || !(query.get(FIELD_QUERY_STRING) instanceof String)) {
            return Optional.empty();
        }
        return Optional.ofNullable(query.getString(FIELD_QUERY_STRING));
    }

    private Document createBackendQuery(String filter) {
        return new BackendQuery(filter);
    }
}
