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
package org.graylog.plugins.views.search.explain;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ExplainService {

    private final StreamQueryExplainer streamQueryExplainer;

    @Inject
    public ExplainService(StreamQueryExplainer streamQueryExplainer) {
        this.streamQueryExplainer = streamQueryExplainer;
    }

    public ExplainResults.QueryExplainResult doExplain(Query query,
                                                       GeneratedQueryContext queryContext,
                                                       QueryBackend.IndexRangesForStreamsInTimeRangeFunction inTimeRangeFunction) {
        final ImmutableMap.Builder<String, ExplainResults.ExplainResult> builder = ImmutableMap.builder();
        query.searchTypes().forEach(searchType -> queryContext.getSearchTypeQueryString(searchType.id()).ifPresent(queryString -> {
            final Set<String> effectiveStreams = query.effectiveStreams(searchType);
            final TimeRange effectiveTimeRange = query.effectiveTimeRange(searchType);
            final Set<IndexRange> indexRanges = inTimeRangeFunction.apply(effectiveStreams, effectiveTimeRange);
            final Set<ExplainResults.IndexRangeResult> affectedIndexRanges = indexRanges.stream()
                    .map(ExplainResults.IndexRangeResult::fromIndexRange)
                    .collect(Collectors.toSet());
            final Set<StreamQueryExplainer.DataRoutedStream> dataRoutedStreams = streamQueryExplainer.explainStreams(effectiveTimeRange, effectiveStreams);

            builder.put(searchType.id(), new ExplainResults.ExplainResult(queryString, affectedIndexRanges, dataRoutedStreams));
        }));

        return new ExplainResults.QueryExplainResult(builder.build());
    }

}
