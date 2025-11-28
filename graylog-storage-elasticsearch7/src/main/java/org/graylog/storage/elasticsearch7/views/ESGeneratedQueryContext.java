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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog.plugins.views.search.errors.SearchError;
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

public class ESGeneratedQueryContext extends IndexerGeneratedQueryContext<SearchSourceBuilder> {

    private final ElasticsearchBackend elasticsearchBackend;
    private final MultiBucketsAggregation.Bucket rowBucket;

    @AssistedInject
    public ESGeneratedQueryContext(
            @Assisted ElasticsearchBackend elasticsearchBackend,
            @Assisted SearchSourceBuilder ssb,
            @Assisted Collection<SearchError> validationErrors,
            @Assisted DateTimeZone timezone,
            FieldTypesLookup fieldTypes) {
        super(new HashMap<>(), new HashSet<>(validationErrors), fieldTypes, timezone, ssb, new HashMap<>());
        this.elasticsearchBackend = elasticsearchBackend;
        this.rowBucket = null;
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
        super(contextMap, new HashSet<>(validationErrors), fieldTypes, timezone, ssb, searchTypeQueries);
        this.elasticsearchBackend = elasticsearchBackend;
        this.rowBucket = rowBucket;
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
        return this.searchTypeQueries.computeIfAbsent(searchType.id(), (ignored) -> ssb.shallowCopy()
                .slice(ssb.slice())
                .query(elasticsearchBackend.generateFilterClause(searchType.filter())
                        .map(filterClause -> (QueryBuilder) new BoolQueryBuilder().must(ssb.query()).must(filterClause))
                        .orElse(ssb.query())));
    }

    public ESGeneratedQueryContext withRowBucket(MultiBucketsAggregation.Bucket rowBucket) {
        return new ESGeneratedQueryContext(elasticsearchBackend, ssb, errors, fieldTypes, rowBucket, searchTypeQueries, contextMap, timezone);
    }

    public Optional<MultiBucketsAggregation.Bucket> rowBucket() {
        return Optional.ofNullable(this.rowBucket);
    }
}
