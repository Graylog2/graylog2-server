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
package org.graylog.storage.elasticsearch7.views;

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
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTimeZone;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ESGeneratedQueryContext implements GeneratedQueryContext {

    private final ElasticsearchBackend elasticsearchBackend;
    private final Map<String, SearchSourceBuilder> searchTypeQueries;
    private final Map<Object, Object> contextMap;
    private final Set<SearchError> errors;
    private final SearchSourceBuilder ssb;

    private final FieldTypesLookup fieldTypes;
    private final MultiBucketsAggregation.Bucket rowBucket;
    private final DateTimeZone timezone;

    @AssistedInject
    public ESGeneratedQueryContext(
            @Assisted ElasticsearchBackend elasticsearchBackend,
            @Assisted SearchSourceBuilder ssb,
            @Assisted Collection<SearchError> validationErrors,
            @Assisted DateTimeZone timezone,
            FieldTypesLookup fieldTypes) {
        this.elasticsearchBackend = elasticsearchBackend;
        this.ssb = ssb;
        this.fieldTypes = fieldTypes;
        this.errors = new HashSet<>(validationErrors);
        this.rowBucket = null;
        this.contextMap = new HashMap<>();
        this.searchTypeQueries = new HashMap<>();
        this.timezone = timezone;
    }

    private ESGeneratedQueryContext(
            ElasticsearchBackend elasticsearchBackend,
            SearchSourceBuilder ssb,
            Collection<SearchError> validationErrors,
            FieldTypesLookup fieldTypes,
            MultiBucketsAggregation.Bucket rowBucket,
            Map<String, SearchSourceBuilder> searchTypeQueries,
            Map<Object, Object> contextMap,
            DateTimeZone timezone) {
        this.elasticsearchBackend = elasticsearchBackend;
        this.ssb = ssb;
        this.fieldTypes = fieldTypes;
        this.errors = new HashSet<>(validationErrors);
        this.rowBucket = rowBucket;
        this.searchTypeQueries = searchTypeQueries;
        this.contextMap = contextMap;
        this.timezone = timezone;
    }

    public interface Factory {
        ESGeneratedQueryContext create(
                ElasticsearchBackend elasticsearchBackend,
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
        return elasticsearchBackend.generateFilterClause(filter);
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

    public ESGeneratedQueryContext withRowBucket(MultiBucketsAggregation.Bucket rowBucket) {
        return new ESGeneratedQueryContext(elasticsearchBackend, ssb, errors, fieldTypes, rowBucket, searchTypeQueries, contextMap, timezone);
    }

    public Optional<MultiBucketsAggregation.Bucket> rowBucket() {
        return Optional.ofNullable(this.rowBucket);
    }

    @Override
    public DateTimeZone timezone() {
        return timezone;
    }
}
