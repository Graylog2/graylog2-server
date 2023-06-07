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

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentage;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.HasAggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ESPercentageHandler extends ESPivotSeriesSpecHandler<Percentage, ValueCount> {
    private static final Logger LOG = LoggerFactory.getLogger(ESCountHandler.class);
    private final ESCountHandler esCountHandler;
    private final ESSumHandler esSumHandler;

    @Inject
    public ESPercentageHandler(ESCountHandler esCountHandler, ESSumHandler esSumHandler) {
        this.esCountHandler = esCountHandler;
        this.esSumHandler = esSumHandler;
    }

    @Nonnull
    @Override
    public List<SeriesAggregationBuilder> doCreateAggregation(String name, Pivot pivot, Percentage percentage, ESSearchTypeHandler<Pivot> searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return Stream.concat(
                seriesHandler(name, pivot, percentage, searchTypeHandler, queryContext).stream(),
                seriesHandler(name + "-root", pivot, percentage, searchTypeHandler, queryContext).stream()
                        .map(r -> SeriesAggregationBuilder.root(r.aggregationBuilder()))
        ).toList();
    }

    private List<SeriesAggregationBuilder> seriesHandler(String name, Pivot pivot, Percentage percentage, ESSearchTypeHandler<Pivot> searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield esSumHandler.doCreateAggregation(name, pivot, seriesSpec, searchTypeHandler, queryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield esCountHandler.doCreateAggregation(name, pivot, seriesSpec, searchTypeHandler, queryContext);
            }
        };
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Percentage percentage,
                                        SearchResponse searchResult,
                                        ValueCount valueCount,
                                        ESSearchTypeHandler<Pivot> searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        final long value;
        if (valueCount == null) {
            LOG.error("Unexpected null aggregation result, returning 0 for the count. This is a bug.");
            value = 0;
        } else if (valueCount instanceof MultiBucketsAggregation.Bucket) {
            value = ((MultiBucketsAggregation.Bucket) valueCount).getDocCount();
        } else if (valueCount instanceof Aggregations) {
            value = searchResult.getHits().getTotalHits().value;
        } else {
            value = valueCount.getValue();
        }

        var totalCount = searchResult.getHits().getTotalHits().value;
        var bucketPercentage = (double) value / totalCount;
        return Stream.of(Value.create(percentage.id(), Percentage.NAME, bucketPercentage));
    }

    @Override
    protected Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, HasAggregations aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = aggTypes(queryContext, pivot).getTypes(spec);
        if (objects == null) {
            if (aggregations instanceof MultiBucketsAggregation.Bucket) {
                return createValueCount(((MultiBucketsAggregation.Bucket) aggregations).getDocCount());
            } else if (aggregations instanceof Missing) {
                return createValueCount(((Missing) aggregations).getDocCount());
            }
        } else {
            // try to saved sub aggregation type. this might fail if we refer to the total result of the entire result instead of a specific
            // value_count aggregation. we'll handle that special case in doHandleResult above
            return aggregations.getAggregations().get(objects.v1);
        }

        return null;
    }

    private Aggregation createValueCount(final Long docCount) {
        return new ValueCount() {
            @Override
            public long getValue() {
                return docCount;
            }

            @Override
            public double value() {
                return docCount;
            }

            @Override
            public String getValueAsString() {
                return docCount.toString();
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }

            @Override
            public Map<String, Object> getMetadata() {
                return null;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
                return null;
            }
        };
    }
}
