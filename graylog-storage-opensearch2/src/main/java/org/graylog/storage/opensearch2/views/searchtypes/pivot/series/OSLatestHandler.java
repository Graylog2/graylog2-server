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
package org.graylog.storage.opensearch2.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHit;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHits;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.ParsedFilter;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.TopHits;
import org.graylog.shaded.opensearch2.org.opensearch.search.sort.SortBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.sort.SortOrder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.SeriesAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class OSLatestHandler extends OSPivotSeriesSpecHandler<Latest, ParsedFilter> {
    private static final String AGG_NAME = "latest_aggregation";

    @Nonnull
    @Override
    public List<SeriesAggregationBuilder> doCreateAggregation(String name, Pivot pivot, Latest latestSpec, OSSearchTypeHandler<Pivot> searchTypeHandler, OSGeneratedQueryContext queryContext) {
        final FilterAggregationBuilder latest = AggregationBuilders.filter(name, QueryBuilders.existsQuery(latestSpec.field()))
                .subAggregation(AggregationBuilders.topHits(AGG_NAME)
                        .size(1)
                        .fetchSource(latestSpec.field(), null)
                        .sort(SortBuilders.fieldSort("timestamp").order(SortOrder.DESC)));
        record(queryContext, pivot, latestSpec, name, ParsedFilter.class);
        return List.of(SeriesAggregationBuilder.metric(latest));
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Latest pivotSpec,
                                        SearchResponse searchResult,
                                        ParsedFilter filterAggregation,
                                        OSSearchTypeHandler<Pivot> searchTypeHandler,
                                        OSGeneratedQueryContext OSGeneratedQueryContext) {
        final TopHits latestAggregation = filterAggregation.getAggregations().get(AGG_NAME);
        final Optional<Value> latestValue = Optional.ofNullable(latestAggregation)
                .map(TopHits::getHits)
                .map(SearchHits::getHits)
                .filter(hits -> hits.length > 0)
                .map(hits -> hits[0])
                .map(SearchHit::getSourceAsMap)
                .map(source -> source.get(pivotSpec.field()))
                .map(value -> Value.create(pivotSpec.id(), Latest.NAME, value));
        return latestValue.stream();
    }
}
