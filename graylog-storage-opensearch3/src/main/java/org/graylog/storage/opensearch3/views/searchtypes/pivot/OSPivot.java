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
import com.google.common.collect.Lists;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.OSSearchTypeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class OSPivot implements OSSearchTypeHandler<Pivot> {
    private static final Logger LOG = LoggerFactory.getLogger(OSPivot.class);
    private static final String AGG_NAME = "agg";

    private final Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers;
    private final Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec>> seriesHandlers;
    private final EffectiveTimeRangeExtractor effectiveTimeRangeExtractor;

    @Inject
    public OSPivot(Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers,
                   Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec>> seriesHandlers,
                   EffectiveTimeRangeExtractor effectiveTimeRangeExtractor) {
        this.bucketHandlers = bucketHandlers;
        this.seriesHandlers = seriesHandlers;
        this.effectiveTimeRangeExtractor = effectiveTimeRangeExtractor;
    }

    @Override
    public void doGenerateQueryPart(Query query, Pivot pivot, OSGeneratedQueryContext queryContext) {
        LOG.debug("Generating aggregation for {}", pivot);
        var searchSourceBuilder = queryContext.searchSourceBuilder(pivot);

        Map<String, Aggregation> rootAggs = new HashMap<>();

        var generateRollups = pivot.rollup() || (pivot.rowGroups().isEmpty() && pivot.columnGroups().isEmpty());

        // add global rollup series if those were requested
        if (generateRollups) {
            seriesStream(pivot, queryContext, "global rollup")
                    .filter(result -> Placement.METRIC.equals(result.placement()))
                    .map(SeriesAggregationBuilder::aggregationBuilder);

            //TODO !!!!
//                    .forEach(searchSourceBuilder::aggregation);
        }

        final BucketSpecHandler.CreatedAggregations<NamedAggregationBuilder> createdAggregations = createPivots(BucketSpecHandler.Direction.Row, query, pivot, pivot.rowGroups(), queryContext);
        final NamedAggregationBuilder rootAggregation = createdAggregations.root();
        final NamedAggregationBuilder leafAggregation = createdAggregations.leaf();
        final List<NamedAggregationBuilder> metrics = createdAggregations.metrics();
        seriesStream(pivot, queryContext, "metrics")
                .forEach(result -> {
                    switch (result.placement()) {
                        case METRIC ->
                                metrics.forEach(metric -> metric.aggregationBuilder().aggregations(result.aggregationName(), result.aggregationBuilder()));
                        case ROW ->
                                rootAggregation.aggregationBuilder().aggregations(result.aggregationName(), result.aggregationBuilder());
                        case ROOT -> {
                            if (!generateRollups) {
                                searchSourceBuilder.aggregations(result.aggregationName(), result.aggregationBuilder());
                            }
                        }
                    }
                });

        if (!pivot.columnGroups().isEmpty()) {
            final BucketSpecHandler.CreatedAggregations<NamedAggregationBuilder> columnsAggregation = createPivots(BucketSpecHandler.Direction.Column, query, pivot, pivot.columnGroups(), queryContext);
            final NamedAggregationBuilder columnsRootAggregation = columnsAggregation.root();
            final NamedAggregationBuilder columnsLeafAggregation = columnsAggregation.leaf();
            final List<NamedAggregationBuilder> columnMetrics = columnsAggregation.metrics();
            seriesStream(pivot, queryContext, "metrics")
                    .forEach(result -> {
                        var name = result.aggregationName();
                        var aggregationBuilder = result.aggregationBuilder();
                        switch (result.placement()) {
                            case COLUMN ->
                                    columnsLeafAggregation.aggregationBuilder().aggregations(name, aggregationBuilder);
                            case METRIC -> columnMetrics.forEach(metric -> metric.aggregationBuilder()
                                    .aggregations(name, aggregationBuilder));
                        }
                    });
            if (leafAggregation != null) {
                leafAggregation.aggregationBuilder().aggregations(columnsLeafAggregation.name(), columnsRootAggregation.aggregationBuilder().build());
            } else {
                searchSourceBuilder.aggregations(columnsRootAggregation.name(), columnsRootAggregation.aggregationBuilder().build());
            }
        }

        if (rootAggregation != null) {
            searchSourceBuilder.aggregations(rootAggregation.name(), rootAggregation.aggregationBuilder().build());
        }

        addTimeStampAggregations(searchSourceBuilder);
    }

    private void addTimeStampAggregations(SearchRequest.Builder searchSourceBuilder) {
        final Aggregation startTimestamp = Aggregation.builder().min(f -> f.field("timestamp")).build();
        final Aggregation endTimestamp = Aggregation.builder().max(f -> f.field("timestamp")).build();
        searchSourceBuilder.aggregations("timestamp-min", startTimestamp);
        searchSourceBuilder.aggregations("timestamp-min", endTimestamp);
    }

    private BucketSpecHandler.CreatedAggregations<NamedAggregationBuilder> createPivots(BucketSpecHandler.Direction direction, Query query, Pivot pivot, List<BucketSpec> pivots, OSGeneratedQueryContext queryContext) {
        NamedAggregationBuilder leaf = null;
        NamedAggregationBuilder root = null;
        final List<NamedAggregationBuilder> metrics = new ArrayList<>();
        for (BucketSpec bucketSpec : Lists.reverse(pivots)) {
            final OSPivotBucketSpecHandler<? extends BucketSpec> bucketHandler = bucketHandlers.get(bucketSpec.type());
            final BucketSpecHandler.CreatedAggregations<NamedAggregationBuilder> bucketAggregations = bucketHandler.createAggregation(direction, AGG_NAME, pivot, bucketSpec, queryContext, query);
            final NamedAggregationBuilder aggregationRoot = bucketAggregations.root();
            final NamedAggregationBuilder aggregationLeaf = bucketAggregations.leaf();
            final List<NamedAggregationBuilder> aggregationMetrics = bucketAggregations.metrics();

            metrics.addAll(aggregationMetrics);

            if (root == null) {
                root = aggregationRoot;
                leaf = aggregationLeaf;
            } else {
                // create new root with old root as nested aggregation
                root = new NamedAggregationBuilder(
                        aggregationRoot.name(),
                        aggregationRoot.aggregationBuilder().aggregations(Map.of(
                                root.name(),
                                root.aggregationBuilder().build()
                        ))
                );
            }
        }

        return BucketSpecHandler.CreatedAggregations.create(root, leaf, metrics);
    }

    private Stream<SeriesAggregationBuilder> seriesStream(Pivot pivot, OSGeneratedQueryContext queryContext, String reason) {
        return pivot.series()
                .stream()
                .distinct()
                .flatMap((seriesSpec) -> {
                    final String seriesName = queryContext.seriesName(seriesSpec, pivot);
                    LOG.debug("Adding {} series '{}' with name '{}'", reason, seriesSpec.type(), seriesName);
                    final OSPivotSeriesSpecHandler<? extends SeriesSpec> esPivotSeriesSpecHandler = seriesHandlers.get(seriesSpec.type());
                    if (esPivotSeriesSpecHandler == null) {
                        throw new IllegalArgumentException("No series handler registered for: " + seriesSpec.type());
                    }
                    return esPivotSeriesSpecHandler.createAggregation(seriesName, pivot, seriesSpec, queryContext).stream();
                });
    }

    @WithSpan
    @Override
    public SearchType.Result doExtractResult(Query query, Pivot pivot, MultiSearchItem<JsonData> queryResult, OSGeneratedQueryContext queryContext) {
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
                .forEach(tuple -> {
                    final ImmutableList<String> rowKeys = tuple.keys();
                    final MultiBucketBase rowBucket = tuple.bucket();
                    final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder()
                            .key(rowKeys)
                            .source("leaf");
                    if (pivot.columnGroups().isEmpty() || pivot.rollup()) {
                        processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), rowBucket, true, "row-leaf");
                    }
                    if (!pivot.columnGroups().isEmpty()) {
                        var contextWithRowBucket = queryContext.withRowBucket(rowBucket);
                        retrieveBuckets(pivot, pivot.columnGroups(), rowBucket)
                                .forEach(columnBucketTuple -> {
                                    final ImmutableList<String> columnKeys = columnBucketTuple.keys();
                                    colGroupNames.add(String.join(", ", Stream.concat(columnKeys.stream(), seriesNames.stream()).toList()));

                                    final MultiBucketBase columnBucket = columnBucketTuple.bucket();

                                    processSeries(rowBuilder, queryResult, contextWithRowBucket, pivot, new ArrayDeque<>(columnKeys), columnBucket, false, "col-leaf");
                                });
                    }
                    resultBuilder.addRow(rowBuilder.build());
                });

        if (!pivot.rowGroups().isEmpty() && pivot.rollup()) {
            final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.of());
            processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), initialBucket, true, "row-inner");
            resultBuilder.addRow(rowBuilder.source("non-leaf").build());
        }

        return resultBuilder.columnNames(Stream.concat(fieldsNames, colGroupNames.stream().distinct().sorted()).toList()).build();
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
