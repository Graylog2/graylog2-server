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
package org.graylog.storage.opensearch3.views.searchtypes.pivot.series;

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentage;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.InitialBucket;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.aggregations.ValueCountAggregate;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

public class OSPercentageHandler extends OSPivotSeriesSpecHandler<Percentage> {
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
    public List<SeriesAggregationBuilder> doCreateAggregation(String name, Pivot pivot, Percentage percentage, OSGeneratedQueryContext queryContext) {
        var aggregation = createNestedSeriesAggregation(name, pivot, percentage, queryContext);
        return Stream.concat(
                aggregation.stream(),
                aggregation.stream().map(r -> SeriesAggregationBuilder.root(r.aggregationBuilder()))
        ).toList();
    }

    private List<SeriesAggregationBuilder> createNestedSeriesAggregation(String name, Pivot pivot, Percentage percentage, OSGeneratedQueryContext queryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osSumHandler.createAggregation(name, pivot, seriesSpec, queryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osCountHandler.createAggregation(name, pivot, seriesSpec, queryContext);
            }
        };
    }

    private Stream<Value> handleNestedSeriesResults(Pivot pivot,
                                                    Percentage percentage,
                                                    MultiSearchItem<JsonData> searchResult,
                                                    Aggregate seriesResult,
                                                    OSGeneratedQueryContext esGeneratedQueryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osSumHandler.handleResult(pivot, seriesSpec, searchResult, seriesResult, esGeneratedQueryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield osCountHandler.handleResult(pivot, seriesSpec, searchResult, seriesResult, esGeneratedQueryContext);
            }
        };
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Percentage percentage,
                                        MultiSearchItem<JsonData> searchResult,
                                        Aggregate agg,
                                        OSGeneratedQueryContext osGeneratedQueryContext) {
        final long value;
        if (agg == null) {
            LOG.error("Unexpected null aggregation result, returning 0 for the count. This is a bug.");
            value = 0;
            // TODO!!!
//        } else if (valueCount instanceof MultiBucketsAggregation.Bucket) {
//            value = ((MultiBucketsAggregation.Bucket) valueCount).getDocCount();
//        } else if (valueCount instanceof Aggregations) {
//            value = searchResult.hits().total().value();
        } else if (agg.isValueCount()) {
            value = (agg.valueCount() == null || agg.valueCount().value() == null) ? 0L : agg.valueCount().value().longValue();
        } else {
            value = 0L; // TODO!!!
        }

        var initialBucket = osGeneratedQueryContext.rowBucket().orElseGet(() -> InitialBucket.create(searchResult));
        var rootResult = extractNestedSeriesAggregation(pivot, percentage, initialBucket, osGeneratedQueryContext);
        var nestedSeriesResult = handleNestedSeriesResults(pivot, percentage, searchResult, rootResult, osGeneratedQueryContext);

        return nestedSeriesResult.map(result -> {
                    var totalResult = (Number) result.value();
                    return value / totalResult.doubleValue();
                })
                .map(bucketPercentage -> Value.create(percentage.id(), Percentage.NAME, bucketPercentage));
    }

    private Aggregate extractNestedSeriesAggregation(Pivot pivot, Percentage percentage, MultiBucketBase aggregations, IndexerGeneratedQueryContext<?> queryContext) {
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
    public Aggregate extractAggregationFromResult(Pivot pivot, PivotSpec spec, MultiBucketBase aggregations, IndexerGeneratedQueryContext<?> queryContext) {
        var result = extractNestedSeriesAggregation(pivot, (Percentage) spec, aggregations, queryContext);

        if (result.isValueCount())
            return result.valueCount().toAggregate();

        if (result.isSum()) {
            return ValueCountAggregate.of(v -> v.value(result.sum().value())).toAggregate();
        }

        throw new IllegalStateException("Unable to parse result: " + result);
    }

}
