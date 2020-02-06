package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import com.google.common.collect.ImmutableMap;
import org.bson.Document;

import java.util.Optional;

class Widget {
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_QUERY_STRING = "query_string";

    private final Document widgetDocument;

    Widget(Document widgetDocument) {
        this.widgetDocument = widgetDocument;
    }

    void mergeFilterIntoQueryIfPresent() {
        filter().ifPresent(filter -> {
            widgetDocument.remove(FIELD_FILTER);
            final String newWidgetQuery = concatenateQueryIfExists(filter);
            setWidgetQuery(newWidgetQuery);
        });
    }

    void mergeQuerySpecsIntoWidget(Query query) {
        query.queryString()
                .filter(queryString -> !queryString.trim().isEmpty())
                .ifPresent(this::mergeQueryString);

    }

    private void mergeQueryString(String queryString) {
        final String newWidgetQuery = concatenateQueryIfExists(queryString);
        setWidgetQuery(newWidgetQuery);
    }

    private void setWidgetQuery(String newWidgetQuery) {
        widgetDocument.put(FIELD_QUERY, createBackendQuery(newWidgetQuery));
    }

    private Optional<String> filter() {
        return Optional.ofNullable(widgetDocument.getString(FIELD_FILTER));
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
        return new Document(ImmutableMap.of(
                "type", "elasticsearch",
                FIELD_QUERY_STRING, filter
        ));
    }
}
