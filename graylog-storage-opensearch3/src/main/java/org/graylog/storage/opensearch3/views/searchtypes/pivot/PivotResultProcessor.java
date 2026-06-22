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
package org.graylog.storage.opensearch3.views.searchtypes.pivot;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Turns an OpenSearch {@link MultiSearchItem} response into a {@link PivotResult} by walking the row and column bucket
 * aggregations, extracting series values for each leaf, and adding the optional rollup row.
 */
class PivotResultProcessor {
    private final Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers;
    private final Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec>> seriesHandlers;
    private final EffectiveTimeRangeExtractor effectiveTimeRangeExtractor;

    PivotResultProcessor(Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers,
                         Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec>> seriesHandlers,
                         EffectiveTimeRangeExtractor effectiveTimeRangeExtractor) {
        this.bucketHandlers = bucketHandlers;
        this.seriesHandlers = seriesHandlers;
        this.effectiveTimeRangeExtractor = effectiveTimeRangeExtractor;
    }

    SearchType.Result extract(Query query, Pivot pivot, MultiSearchItem<JsonData> queryResult, OSGeneratedQueryContext queryContext) {
        final AbsoluteRange effectiveTimerange = this.effectiveTimeRangeExtractor.extract(queryResult, query, pivot);

        final var fieldsNames = pivot.rowGroups().stream().flatMap(bs -> bs.fields().stream());
        final var seriesNames = pivot.series().stream().map(SeriesSpec::id).toList();

        final List<String> colGroupNames = pivot.columnGroups().isEmpty() ? seriesNames : new ArrayList<>();

        final PivotResult.Builder resultBuilder = PivotResult.builder()
                .id(pivot.id())
                .effectiveTimerange(effectiveTimerange)
                .total(extractDocumentCount(queryResult));

        pivot.name().ifPresent(resultBuilder::name);

        final InitialBucket initialBucket = InitialBucket.create(queryResult);

        retrieveBuckets(pivot, pivot.rowGroups(), initialBucket)
                .forEach(tuple -> resultBuilder.addRow(buildRow(pivot, queryResult, queryContext, seriesNames, colGroupNames, tuple)));

        if (!pivot.rowGroups().isEmpty() && pivot.rollup()) {
            resultBuilder.addRow(buildRollupRow(pivot, queryResult, queryContext, initialBucket));
        }

        return resultBuilder.columnNames(Stream.concat(fieldsNames, colGroupNames.stream().distinct().sorted()).toList()).build();
    }

    private PivotResult.Row buildRow(Pivot pivot,
                                     MultiSearchItem<JsonData> queryResult,
                                     OSGeneratedQueryContext queryContext,
                                     List<String> seriesNames,
                                     List<String> colGroupNames,
                                     PivotBucket tuple) {
        final ImmutableList<String> rowKeys = tuple.keys();
        final MultiBucketBase rowBucket = tuple.bucket();
        final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder()
                .key(rowKeys)
                .source("leaf");
        if (pivot.columnGroups().isEmpty() || pivot.rollup()) {
            processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), rowBucket, true, "row-leaf");
        }
        if (!pivot.columnGroups().isEmpty()) {
            final var contextWithRowBucket = queryContext.withRowBucket(rowBucket);
            retrieveBuckets(pivot, pivot.columnGroups(), rowBucket)
                    .forEach(columnBucketTuple -> {
                        final ImmutableList<String> columnKeys = columnBucketTuple.keys();
                        colGroupNames.add(String.join(", ", Stream.concat(columnKeys.stream(), seriesNames.stream()).toList()));

                        final MultiBucketBase columnBucket = columnBucketTuple.bucket();

                        processSeries(rowBuilder, queryResult, contextWithRowBucket, pivot, new ArrayDeque<>(columnKeys), columnBucket, false, "col-leaf");
                    });
        }
        return rowBuilder.build();
    }

    private PivotResult.Row buildRollupRow(Pivot pivot,
                                           MultiSearchItem<JsonData> queryResult,
                                           OSGeneratedQueryContext queryContext,
                                           InitialBucket initialBucket) {
        final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.of());
        processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), initialBucket, true, "row-inner");
        return rowBuilder.source("non-leaf").build();
    }

    private Stream<PivotBucket> retrieveBuckets(Pivot pivot, List<BucketSpec> pivots, MultiBucketBase aggregations) {
        Stream<PivotBucket> result = Stream.of(PivotBucket.create(ImmutableList.of(), aggregations));

        for (BucketSpec bucketSpec : pivots) {
            result = result.flatMap((tuple) -> {
                final OSPivotBucketSpecHandler<? extends BucketSpec> bucketHandler = bucketHandlers.get(bucketSpec.type());
                return bucketHandler.extractBuckets(pivot, bucketSpec, tuple);
            });
        }

        return result;
    }

    private void processSeries(PivotResult.Row.Builder rowBuilder,
                               MultiSearchItem<JsonData> searchResult,
                               OSGeneratedQueryContext queryContext,
                               Pivot pivot,
                               ArrayDeque<String> columnKeys,
                               MultiBucketBase aggregation,
                               boolean rollup,
                               String source) {
        pivot.series().forEach(seriesSpec -> {
            final OSPivotSeriesSpecHandler<? extends SeriesSpec> seriesHandler = seriesHandlers.get(seriesSpec.type());
            final Aggregate series = seriesHandler.extractAggregationFromResult(pivot, seriesSpec, aggregation, queryContext);
            seriesHandler.handleResult(pivot, seriesSpec, searchResult, series, queryContext)
                    .map(value -> {
                        columnKeys.addLast(value.id());
                        final PivotResult.Value v = PivotResult.Value.create(columnKeys, value.value(), rollup, source);
                        columnKeys.removeLast();
                        return v;
                    })
                    .forEach(rowBuilder::addValue);
        });
    }

    private long extractDocumentCount(MultiSearchItem<JsonData> queryResult) {
        return queryResult.hits().total().value();
    }
}
