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
package org.graylog.storage.opensearch2.views.searchtypes;

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;

import java.util.Optional;

public class OSEventListDelegate implements OSSearchTypeHandler<EventList> {
    private final EventListStrategy strategy;

    @Inject
    public OSEventListDelegate(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<EventListStrategy> strategy,
                               OSEventList osEventList) {
        this.strategy = strategy.orElse(osEventList);
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, EventList searchType, SearchResponse queryResult, Aggregations aggregations, OSGeneratedQueryContext queryContext) {
        return strategy.doExtractResult(job, query, searchType, queryResult, aggregations, queryContext);
    }

    @Override
    public void doGenerateQueryPart(Query query, EventList searchType, OSGeneratedQueryContext queryContext) {
        strategy.doGenerateQueryPart(query, searchType, queryContext);
    }
}
