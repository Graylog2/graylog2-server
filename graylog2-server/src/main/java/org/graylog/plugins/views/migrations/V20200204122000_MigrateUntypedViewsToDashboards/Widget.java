/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;
import org.elasticsearch.common.util.set.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class Widget {
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_QUERY_STRING = "query_string";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_ID = "id";

    private final Document widgetDocument;

    Widget(Document widgetDocument) {
        this.widgetDocument = widgetDocument;
    }

    void mergeFilterIntoQueryIfPresent() {
        filter().ifPresent(filter -> {
            widgetDocument.remove(FIELD_FILTER);
            mergeQueryString(filter);
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

    Optional<Document> query() {
        return Optional.ofNullable(widgetDocument.get(FIELD_QUERY, Document.class));
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

    Optional<Document> timerange() { return Optional.ofNullable(widgetDocument.get(FIELD_TIMERANGE, Document.class)); }

    Set<String> streams() {
        @SuppressWarnings("unchecked") final Collection<String> streams = widgetDocument.get(FIELD_STREAMS, Collection.class);
        return streams == null ? Collections.emptySet() : new HashSet<>(streams);
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

    String id() {
        return this.widgetDocument.getString(FIELD_ID);
    }
}
