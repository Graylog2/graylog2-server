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
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.InitialBucket;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;

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
        var aggregation = createNestedSeriesAggregation(name, pivot, percentage, searchTypeHandler, queryContext);
        return Stream.concat(
                aggregation.stream(),
                aggregation.stream().map(r -> SeriesAggregationBuilder.root(r.aggregationBuilder()))
        ).toList();
    }

    private List<SeriesAggregationBuilder> createNestedSeriesAggregation(String name, Pivot pivot, Percentage percentage, ESSearchTypeHandler<Pivot> searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield esSumHandler.createAggregation(name, pivot, seriesSpec, searchTypeHandler, queryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield esCountHandler.createAggregation(name, pivot, seriesSpec, searchTypeHandler, queryContext);
            }
        };
    }

    private Stream<Value> handleNestedSeriesResults(Pivot pivot,
                                                    Percentage percentage,
                                                    SearchResponse searchResult,
                                                    Object seriesResult,
                                                    ESSearchTypeHandler<Pivot> searchTypeHandler,
                                                    ESGeneratedQueryContext esGeneratedQueryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield esSumHandler.handleResult(pivot, seriesSpec, searchResult, seriesResult, searchTypeHandler, esGeneratedQueryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield esCountHandler.handleResult(pivot, seriesSpec, searchResult, seriesResult, searchTypeHandler, esGeneratedQueryContext);
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

        var initialBucket = esGeneratedQueryContext.rowBucket().orElseGet(() -> InitialBucket.create(searchResult));
        var rootResult = extractNestedSeriesAggregation(pivot, percentage, initialBucket, esGeneratedQueryContext);
        var nestedSeriesResult = handleNestedSeriesResults(pivot, percentage, searchResult, rootResult, searchTypeHandler, esGeneratedQueryContext);

        return nestedSeriesResult.map(result -> {
                    var totalResult = (Number) result.value();
                    return value / totalResult.doubleValue();
                })
                .map(bucketPercentage -> Value.create(percentage.id(), Percentage.NAME, bucketPercentage));
    }

    private Aggregation extractNestedSeriesAggregation(Pivot pivot, Percentage percentage, HasAggregations aggregations, ESGeneratedQueryContext queryContext) {
        return switch (percentage.strategy().orElse(Percentage.Strategy.COUNT)) {
            case SUM -> {
                var seriesSpecBuilder = Sum.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield esSumHandler.extractAggregationFromResult(pivot, seriesSpec, aggregations, queryContext);
            }
            case COUNT -> {
                var seriesSpecBuilder = Count.builder().id(percentage.id());
                var seriesSpec = percentage.field().map(seriesSpecBuilder::field).orElse(seriesSpecBuilder).build();

                yield esCountHandler.extractAggregationFromResult(pivot, seriesSpec, aggregations, queryContext);
            }
        };
    }

    @Override
    public Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, HasAggregations aggregations, ESGeneratedQueryContext queryContext) {
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
