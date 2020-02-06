package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Traverser;
import org.bson.Document;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.stream.Collectors.toSet;

class Query {
    private static final String FIELD_ID = "id";
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_SEARCH_TYPE_ID = "id";
    private static final String FIELD_SEARCH_TYPES = "search_types";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_QUERY_STRING = "query_string";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_SUB_FILTERS = "filters";
    private static final String FIELD_FILTER_TYPE = "type";
    private static final String TYPE_STREAM_FILTER = "stream";
    private static final String FIELD_STREAM_ID = "id";

    private final Document queryDocument;

    Query(Document queryDocument) {
        this.queryDocument = queryDocument;
    }

    String id() {
        return this.queryDocument.getString(FIELD_ID);
    }

    private Set<Document> searchTypesByIds(Set<String> searchTypeIds) {
        @SuppressWarnings("unchecked") final Set<Document> searchTypes = queryDocument.get(FIELD_SEARCH_TYPES, Set.class);
        if (searchTypes == null) {
            return Collections.emptySet();
        }

        return searchTypes.stream()
                .filter(searchType -> {
                    final String searchTypeId = searchType.getString(FIELD_SEARCH_TYPE_ID);
                    return searchTypeId != null && searchTypeIds.contains(searchTypeId);
                })
                .collect(Collectors.toSet());
    }

    Optional<String> queryString() {
        if (!queryDocument.containsKey(FIELD_QUERY)) {
            return Optional.empty();
        }
        final Document query = queryDocument.get(FIELD_QUERY, Document.class);
        if (!query.containsKey(FIELD_QUERY_STRING) || !(query.get(FIELD_QUERY_STRING) instanceof String)) {
            return Optional.empty();
        }
        return Optional.ofNullable(query.getString(FIELD_QUERY_STRING));

    }

    Optional<Document> timeRange() {
        if (!queryDocument.containsKey(FIELD_TIMERANGE)) {
            return Optional.empty();
        }
        return Optional.ofNullable(queryDocument.get(FIELD_TIMERANGE, Document.class));
    }

    private Set<Document> subFiltersOfFilter(Document filter) {
        final Set<Document> subfilters = filter.get(FIELD_SUB_FILTERS, Set.class);
        return firstNonNull(subfilters, Collections.emptySet());
    }

    private boolean isStreamFilter(Document filter) {
        if (!filter.containsKey(FIELD_FILTER_TYPE)) {
            return false;
        }
        return TYPE_STREAM_FILTER.equals(filter.getString(FIELD_FILTER_TYPE));
    }

    private String extractStreamIdFromStreamFilter(Document filter) {
        return filter.getString(FIELD_STREAM_ID);
    }

    Set<String> streams() {
        final Document optionalFilter = this.queryDocument.get(FIELD_FILTER, Document.class);
        return Optional.ofNullable(optionalFilter)
                .map(filter -> {
                    @SuppressWarnings("UnstableApiUsage") final Traverser<Document> filterTraverser = Traverser.forTree(this::subFiltersOfFilter);
                    return StreamSupport.stream(filterTraverser.breadthFirst(filter).spliterator(), false)
                            .filter(this::isStreamFilter)
                            .map(this::extractStreamIdFromStreamFilter)
                            .filter(Objects::nonNull)
                            .collect(toSet());
                })
                .orElseGet(Collections::emptySet);
    }

    public void clearUnwantedProperties() {
        this.queryDocument.put(FIELD_FILTER, new Document());
        this.queryDocument.put(FIELD_QUERY, new BackendQuery(""));
        this.queryDocument.put(FIELD_TIMERANGE, new Document(ImmutableMap.of(
                "type", "relative",
                "range", 300
        )));
    }
}
