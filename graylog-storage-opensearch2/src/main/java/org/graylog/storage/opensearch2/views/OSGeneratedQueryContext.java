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
package org.graylog.storage.opensearch2.views;

import com.google.common.base.MoreObjects;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTimeZone;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OSGeneratedQueryContext implements GeneratedQueryContext {
    private final OpenSearchBackend openSearchBackend;
    private final Map<String, SearchSourceBuilder> searchTypeQueries;
    private final Map<Object, Object> contextMap;
    private final Set<SearchError> errors;
    private final SearchSourceBuilder ssb;

    private final FieldTypesLookup fieldTypes;

    private final MultiBucketsAggregation.Bucket rowBucket;

    private final DateTimeZone timezone;

    @AssistedInject
    public OSGeneratedQueryContext(
            @Assisted OpenSearchBackend elasticsearchBackend,
            @Assisted SearchSourceBuilder ssb,
            @Assisted Collection<SearchError> validationErrors,
            @Assisted DateTimeZone timezone,
            FieldTypesLookup fieldTypes) {
        this.openSearchBackend = elasticsearchBackend;
        this.ssb = ssb;
        this.fieldTypes = fieldTypes;
        this.errors = new HashSet<>(validationErrors);
        this.timezone = timezone;
        this.rowBucket = null;
        this.contextMap = new HashMap<>();
        this.searchTypeQueries = new HashMap<>();
    }

    private OSGeneratedQueryContext(OpenSearchBackend openSearchBackend,
                                    SearchSourceBuilder ssb,
                                    Set<SearchError> errors,
                                    FieldTypesLookup fieldTypes,
                                    MultiBucketsAggregation.Bucket rowBucket,
                                    Map<String, SearchSourceBuilder> searchTypeQueries,
                                    Map<Object, Object> contextMap,
                                    DateTimeZone timezone) {
        this.openSearchBackend = openSearchBackend;
        this.ssb = ssb;
        this.errors = errors;
        this.fieldTypes = fieldTypes;
        this.rowBucket = rowBucket;
        this.searchTypeQueries = searchTypeQueries;
        this.contextMap = contextMap;
        this.timezone = timezone;
    }

    public interface Factory {
        OSGeneratedQueryContext create(
                OpenSearchBackend elasticsearchBackend,
                SearchSourceBuilder ssb,
                Collection<SearchError> validationErrors,
                DateTimeZone timezone
        );
    }

    public SearchSourceBuilder searchSourceBuilder(SearchType searchType) {
        return this.searchTypeQueries.computeIfAbsent(searchType.id(), (ignored) -> {
            final QueryBuilder queryBuilder = generateFilterClause(searchType.filter())
                    .map(filterClause -> (QueryBuilder)new BoolQueryBuilder().must(ssb.query()).must(filterClause))
                    .orElse(ssb.query());
            return ssb.shallowCopy()
                    .slice(ssb.slice())
                    .query(queryBuilder);
        });
    }

    Map<String, SearchSourceBuilder> searchTypeQueries() {
        return this.searchTypeQueries;
    }

    @Override
    public Optional<String> getSearchTypeQueryString(String id) {
        return Optional.ofNullable(searchTypeQueries.get(id)).map(SearchSourceBuilder::toString);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("elasticsearch query", ssb)
                .toString();
    }

    public Map<Object, Object> contextMap() {
        return contextMap;
    }

    private Optional<QueryBuilder> generateFilterClause(Filter filter) {
        return openSearchBackend.generateFilterClause(filter);
    }

    public String seriesName(SeriesSpec seriesSpec, Pivot pivot) {
        return pivot.id() + "-series-" + seriesSpec.id();
    }

    public Optional<String> fieldType(Set<String> streamIds, String field) {
        return fieldTypes.getType(streamIds, field);
    }

    @Override
    public void addError(SearchError error) {
        errors.add(error);
    }

    @Override
    public Collection<SearchError> errors() {
        return errors;
    }

    public OSGeneratedQueryContext withRowBucket(MultiBucketsAggregation.Bucket rowBucket) {
        return new OSGeneratedQueryContext(openSearchBackend, ssb, errors, fieldTypes, rowBucket, searchTypeQueries, contextMap, timezone);
    }

    public Optional<MultiBucketsAggregation.Bucket> rowBucket() {
        return Optional.ofNullable(this.rowBucket);
    }

    @Override
    public DateTimeZone timezone() {
        return timezone;
    }
}
