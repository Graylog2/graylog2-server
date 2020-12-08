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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.util.UniqueNamer;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ESGeneratedQueryContext implements GeneratedQueryContext {

    private final ElasticsearchBackend elasticsearchBackend;
    private final Map<String, SearchSourceBuilder> searchTypeQueries = Maps.newHashMap();
    private Map<Object, Object> contextMap = Maps.newHashMap();
    private final UniqueNamer uniqueNamer = new UniqueNamer("agg-");
    private Set<SearchError> errors = Sets.newHashSet();
    private final SearchSourceBuilder ssb;
    private final SearchJob job;
    private final Query query;
    private final Set<QueryResult> results;

    private final FieldTypesLookup fieldTypes;

    @AssistedInject
    public ESGeneratedQueryContext(
            @Assisted ElasticsearchBackend elasticsearchBackend,
            @Assisted SearchSourceBuilder ssb,
            @Assisted SearchJob job,
            @Assisted Query query,
            @Assisted Set<QueryResult> results,
            FieldTypesLookup fieldTypes) {
        this.elasticsearchBackend = elasticsearchBackend;
        this.ssb = ssb;
        this.job = job;
        this.query = query;
        this.results = results;
        this.fieldTypes = fieldTypes;
    }

    public interface Factory {
        ESGeneratedQueryContext create(
                ElasticsearchBackend elasticsearchBackend,
                SearchSourceBuilder ssb,
                SearchJob job,
                Query query,
                Set<QueryResult> results
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
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("elasticsearch query", ssb)
                .toString();
    }

    public Map<Object, Object> contextMap() {
        return contextMap;
    }

    public String nextName() {
        return uniqueNamer.nextName();
    }

    private Optional<QueryBuilder> generateFilterClause(Filter filter) {
        return elasticsearchBackend.generateFilterClause(filter, job, query, results);
    }

    public String seriesName(SeriesSpec seriesSpec, Pivot pivot) {
        return pivot.id() + "-series-" + seriesSpec.literal();
    }

    public void addAggregation(AggregationBuilder builder, SearchType searchType) {
        this.searchTypeQueries().get(searchType.id()).aggregation(builder);
    }

    public void addAggregations(Collection<AggregationBuilder> builders, SearchType searchType) {
        builders.forEach(builder -> this.searchTypeQueries().get(searchType.id()).aggregation(builder));
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
}
