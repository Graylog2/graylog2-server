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

public class ESGeneratedQueryContext extends IndexerGeneratedQueryContext<SearchSourceBuilder, MultiBucketsAggregation.Bucket> {

    private final ElasticsearchBackend elasticsearchBackend;

    @AssistedInject
    public ESGeneratedQueryContext(
            @Assisted ElasticsearchBackend elasticsearchBackend,
            @Assisted SearchSourceBuilder ssb,
            @Assisted Collection<SearchError> validationErrors,
            @Assisted DateTimeZone timezone,
            FieldTypesLookup fieldTypes) {
        super(new HashMap<>(), new HashSet<>(validationErrors), fieldTypes, timezone, ssb, new HashMap<>());
        this.elasticsearchBackend = elasticsearchBackend;
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
}
