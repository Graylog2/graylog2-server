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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHits;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.TopHits;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortOrder;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.SeriesAggregationBuilder;

import java.util.Optional;

public class ESLatestHandler extends ESBasicSeriesSpecHandler<Latest, ParsedFilter> {
    private static final String AGG_NAME = "latest_aggregation";

    @Override
    protected SeriesAggregationBuilder createAggregationBuilder(final String name, final Latest latestSpec) {
        final FilterAggregationBuilder latest = AggregationBuilders.filter(name, QueryBuilders.existsQuery(latestSpec.field()))
                .subAggregation(AggregationBuilders.topHits(AGG_NAME)
                        .size(1)
                        .fetchSource(latestSpec.field(), null)
                        .sort(SortBuilders.fieldSort("timestamp").order(SortOrder.DESC)));
        return SeriesAggregationBuilder.metric(latest);
    }

    @Override
    protected Object getValueFromAggregationResult(final ParsedFilter filterAggregation, final Latest seriesSpec) {
        final TopHits latestAggregation = filterAggregation.getAggregations().get(AGG_NAME);
        return Optional.ofNullable(latestAggregation)
                .map(TopHits::getHits)
                .map(SearchHits::getHits)
                .filter(hits -> hits.length > 0)
                .map(hits -> hits[0])
                .map(SearchHit::getSourceAsMap)
                .map(source -> source.get(seriesSpec.field()))
                .orElse(null);
    }
}
