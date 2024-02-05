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
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentage;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.ParsedSum;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.ValueCount;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.InitialBucket;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class OSPercentageHandler extends OSPivotSeriesSpecHandler<Percentage, ValueCount> {
    private static final Logger LOG = LoggerFactory.getLogger(OSCountHandler.class);
    private final OSCountHandler osCountHandler;
    private final OSSumHandler osSumHandler;

    @Inject
    public OSPercentageHandler(OSCountHandler osCountHandler, OSSumHandler osSumHandler) {
        this.osCountHandler = osCountHandler;
        this.osSumHandler = osSumHandler;
    }

    @Nonnull
    @Override
    public List<SeriesAggregationBuilder> doCreateAggregation(String name, Pivot pivot, Percentage percentage, OSSearchTypeHandler<Pivot> searchTypeHandler, OSGeneratedQueryContext queryContext) {
        var aggregation = createNestedSeriesAggregation(name, pivot, percentage, searchTypeHandler, queryContext);
        return Stream.concat(
                aggregation.stream(),
                aggregation.stream().map(r -> SeriesAggregationBuilder.root(r.aggregationBuilder()))
        ).toList();
    }

    private List<SeriesAggregationBuilder> createNestedSeriesAggregation(String name, Pivot pivot, Percentage percentage, OSSearchTypeHandler<Pivot> searchTypeHandler, OSGeneratedQueryContext queryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osSumHandler.createAggregation(name, pivot, seriesSpec, searchTypeHandler, queryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osCountHandler.createAggregation(name, pivot, seriesSpec, searchTypeHandler, queryContext);
            }
        };
    }

    private Stream<Value> handleNestedSeriesResults(Pivot pivot,
                                                    Percentage percentage,
                                                    SearchResponse searchResult,
                                                    Object seriesResult,
                                                    OSSearchTypeHandler<Pivot> searchTypeHandler,
                                                    OSGeneratedQueryContext esGeneratedQueryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osSumHandler.handleResult(pivot, seriesSpec, searchResult, seriesResult, searchTypeHandler, esGeneratedQueryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osCountHandler.handleResult(pivot, seriesSpec, searchResult, seriesResult, searchTypeHandler, esGeneratedQueryContext);
            }
        };
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Percentage percentage,
                                        SearchResponse searchResult,
                                        ValueCount valueCount,
                                        OSSearchTypeHandler<Pivot> searchTypeHandler,
                                        OSGeneratedQueryContext osGeneratedQueryContext) {
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

        var initialBucket = osGeneratedQueryContext.rowBucket().orElseGet(() -> InitialBucket.create(searchResult));
        var rootResult = extractNestedSeriesAggregation(pivot, percentage, initialBucket, osGeneratedQueryContext);
        var nestedSeriesResult = handleNestedSeriesResults(pivot, percentage, searchResult, rootResult, searchTypeHandler, osGeneratedQueryContext);

        return nestedSeriesResult.map(result -> {
                    var totalResult = (Number) result.value();
                    return value / totalResult.doubleValue();
                })
                .map(bucketPercentage -> Value.create(percentage.id(), Percentage.NAME, bucketPercentage));
    }

    private Aggregation extractNestedSeriesAggregation(Pivot pivot, Percentage percentage, HasAggregations aggregations, OSGeneratedQueryContext queryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osSumHandler.extractAggregationFromResult(pivot, seriesSpec, aggregations, queryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osCountHandler.extractAggregationFromResult(pivot, seriesSpec, aggregations, queryContext);
            }
        };
    }

    @Override
    public Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, HasAggregations aggregations, OSGeneratedQueryContext queryContext) {
        var result = extractNestedSeriesAggregation(pivot, (Percentage) spec, aggregations, queryContext);
        if (result instanceof ValueCount) {
            return result;
        }
        if (result instanceof ParsedSum sum) {
            return createValueCount(sum.getValue());
        }

        throw new IllegalStateException("Unable to parse result: " + result);
    }

    private Aggregation createValueCount(final Double value) {
        return new ValueCount() {
            @Override
            public long getValue() {
                return value.longValue();
            }

            @Override
            public double value() {
                return value;
            }

            @Override
            public String getValueAsString() {
                return value.toString();
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
            public XContentBuilder toXContent(XContentBuilder xContentBuilder, Params params) {
                return null;
            }
        };
    }
}
