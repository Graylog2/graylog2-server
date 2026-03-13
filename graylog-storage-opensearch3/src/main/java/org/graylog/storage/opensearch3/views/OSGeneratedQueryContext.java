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
package org.graylog.storage.opensearch3.views;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog.plugins.views.search.errors.SearchError;
import org.joda.time.DateTimeZone;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OSGeneratedQueryContext extends IndexerGeneratedQueryContext<MutableSearchRequestBuilder> {
    private final OpenSearchBackend openSearchBackend;
    private final MultiBucketBase rowBucket;

    @AssistedInject
    public OSGeneratedQueryContext(
            @Assisted OpenSearchBackend elasticsearchBackend,
            @Assisted MutableSearchRequestBuilder ssb,
            @Assisted Collection<SearchError> validationErrors,
            @Assisted DateTimeZone timezone,
            FieldTypesLookup fieldTypes) {
        super(new HashMap<>(), new HashSet<>(validationErrors), fieldTypes, timezone, ssb, new HashMap<>());
        this.openSearchBackend = elasticsearchBackend;
        this.rowBucket = null;
    }

    private OSGeneratedQueryContext(OpenSearchBackend openSearchBackend,
                                    MutableSearchRequestBuilder ssb,
                                    Set<SearchError> errors,
                                    FieldTypesLookup fieldTypes,
                                    MultiBucketBase rowBucket,
                                    Map<String, MutableSearchRequestBuilder> searchTypeQueries,
                                    Map<Object, Object> contextMap,
                                    DateTimeZone timezone) {
        super(contextMap, new HashSet<>(errors), fieldTypes, timezone, ssb, searchTypeQueries);
        this.openSearchBackend = openSearchBackend;
        this.rowBucket = rowBucket;
    }

    public interface Factory {
        OSGeneratedQueryContext create(
                OpenSearchBackend elasticsearchBackend,
                MutableSearchRequestBuilder ssb,
                Collection<SearchError> validationErrors,
                DateTimeZone timezone
        );
    }

    public MutableSearchRequestBuilder searchSourceBuilder(SearchType searchType) {
        return this.searchTypeQueries.computeIfAbsent(searchType.id(), ignored ->
                ssb.copy().query(
                        openSearchBackend.generateFilterQuery(searchType.filter())
                                .map(filterClause ->
                                        QueryBuilders.bool()
                                                .must(ssb.query())
                                                .must(filterClause)
                                                .build().toQuery())
                                .orElse(ssb.query())
                ));
    }

    public OSGeneratedQueryContext withRowBucket(MultiBucketBase rowBucket) {
        return new OSGeneratedQueryContext(openSearchBackend, ssb, errors, fieldTypes, rowBucket, searchTypeQueries, contextMap, timezone);
    }

    public Optional<MultiBucketBase> rowBucket() {
        return Optional.ofNullable(this.rowBucket);
    }

}
