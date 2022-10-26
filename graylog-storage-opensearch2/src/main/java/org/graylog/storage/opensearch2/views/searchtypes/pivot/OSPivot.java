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
package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import com.google.common.collect.ImmutableList;
import one.util.streamex.EntryStream;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class OSPivot implements OSSearchTypeHandler<Pivot> {
    private static final Logger LOG = LoggerFactory.getLogger(OSPivot.class);
    private static final String AGG_NAME = "agg";

    private final Map<String, OSPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers;
    private final Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;
    private final EffectiveTimeRangeExtractor effectiveTimeRangeExtractor;

    @Inject
    public OSPivot(Map<String, OSPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers,
                   Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers,
                   EffectiveTimeRangeExtractor effectiveTimeRangeExtractor) {
        this.bucketHandlers = bucketHandlers;
        this.seriesHandlers = seriesHandlers;
        this.effectiveTimeRangeExtractor = effectiveTimeRangeExtractor;
    }

    @Override
    public void doGenerateQueryPart(Query query, Pivot pivot, OSGeneratedQueryContext queryContext) {
        LOG.debug("Generating aggregation for {}", pivot);
        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(pivot);

        final Map<Object, Object> contextMap = queryContext.contextMap();
        final AggTypes aggTypes = new AggTypes();
        contextMap.put(pivot.id(), aggTypes);

        // add global rollup series if those were requested
        if (pivot.rollup()) {
            seriesStream(pivot, queryContext, "global rollup")
                    .forEach(searchSourceBuilder::aggregation);
        }

        final BucketSpecHandler.CreatedAggregations<AggregationBuilder> createdAggregations = createPivots(query, pivot, pivot.rowGroups(), queryContext);
        final AggregationBuilder rootAggregation = createdAggregations.root();
        final AggregationBuilder leafAggregation = createdAggregations.leaf();
        final List<AggregationBuilder> metricsAggregations = createdAggregations.metrics();
        if (!pivot.rowGroups().isEmpty() && (pivot.columnGroups().isEmpty() || pivot.rollup())) {
            seriesStream(pivot, queryContext, "metrics")
                    .forEach(aggregation -> metricsAggregations.forEach(metricsAggregation -> metricsAggregation.subAggregation(aggregation)));
        }

        if (!pivot.columnGroups().isEmpty()) {
            final BucketSpecHandler.CreatedAggregations<AggregationBuilder> createdColumnsAggregations = createPivots(query, pivot, pivot.columnGroups(), queryContext);
            final AggregationBuilder columnsRootAggregation = createdColumnsAggregations.root();
            final List<AggregationBuilder> columnMetricsAggregations = createdColumnsAggregations.metrics();
            seriesStream(pivot, queryContext, "metrics")
                    .forEach(aggregation -> columnMetricsAggregations.forEach(metricsAggregation -> metricsAggregation.subAggregation(aggregation)));
            if (leafAggregation != null) {
                leafAggregation.subAggregation(columnsRootAggregation);
            } else {
                searchSourceBuilder.aggregation(columnsRootAggregation);
            }
        }

        if (rootAggregation != null) {
            searchSourceBuilder.aggregation(rootAggregation);
        }

        addTimeStampAggregations(searchSourceBuilder);
    }

    private void addTimeStampAggregations(SearchSourceBuilder searchSourceBuilder) {
        final MinAggregationBuilder startTimestamp = AggregationBuilders.min("timestamp-min").field("timestamp");
        final MaxAggregationBuilder endTimestamp = AggregationBuilders.max("timestamp-max").field("timestamp");
        searchSourceBuilder.aggregation(startTimestamp);
        searchSourceBuilder.aggregation(endTimestamp);
    }

    private List<Tuple2<String, List<BucketSpec>>> groupByConsecutiveType(List<BucketSpec> pivots) {
        final List<Tuple2<String, List<BucketSpec>>> groups = new ArrayList<>();

        List<BucketSpec> currentBuckets = new ArrayList<>();
        String currentType = null;

        for (BucketSpec bucketSpec : pivots) {
            if (bucketSpec.type().equals(currentType)) {
                currentBuckets.add(bucketSpec);
            } else {
                if (!currentBuckets.isEmpty()) {
                    groups.add(new Tuple2<>(currentType, currentBuckets));
                }

                currentBuckets = new ArrayList<>();
                currentBuckets.add(bucketSpec);
                currentType = bucketSpec.type();
            }
        }

        if (!currentBuckets.isEmpty()) {
            groups.add(new Tuple2<>(currentType, currentBuckets));
        }

        return groups;
    }

    private BucketSpecHandler.CreatedAggregations<AggregationBuilder> createPivots(Query query, Pivot pivot, List<BucketSpec> pivots, OSGeneratedQueryContext queryContext) {
        AggregationBuilder leaf = null;
        AggregationBuilder root = null;
        final List<AggregationBuilder> metrics = new ArrayList<>();
        for (Tuple2<String, List<BucketSpec>> group : groupByConsecutiveType(pivots)) {
            final OSPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> bucketHandler = bucketHandlers.get(group.v1());
            final Optional<BucketSpecHandler.CreatedAggregations<AggregationBuilder>> bucketAggregations = bucketHandler.createAggregation(AGG_NAME, pivot, group.v2(), queryContext, query);
            final AggregationBuilder aggregationRoot = bucketAggregations.map(BucketSpecHandler.CreatedAggregations::root).orElse(null);
            final AggregationBuilder aggregationLeaf = bucketAggregations.map(BucketSpecHandler.CreatedAggregations::leaf).orElse(null);
            final List<AggregationBuilder> aggregationMetrics = bucketAggregations.map(BucketSpecHandler.CreatedAggregations::metrics).orElse(Collections.emptyList());

            metrics.addAll(aggregationMetrics);
            if (root == null && leaf == null) {
                root = aggregationRoot;
                leaf = aggregationLeaf;
            } else {
                leaf.subAggregation(aggregationRoot);
                leaf = aggregationLeaf;
            }
        }

        return BucketSpecHandler.CreatedAggregations.create(root, leaf, metrics);
    }

    private Stream<AggregationBuilder> seriesStream(Pivot pivot, OSGeneratedQueryContext queryContext, String reason) {
        return EntryStream.of(pivot.series())
                .mapKeyValue((integer, seriesSpec) -> {
                    final String seriesName = queryContext.seriesName(seriesSpec, pivot);
                    LOG.debug("Adding {} series '{}' with name '{}'", reason, seriesSpec.type(), seriesName);
                    final OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> esPivotSeriesSpecHandler = seriesHandlers.get(seriesSpec.type());
                    if (esPivotSeriesSpecHandler == null) {
                        throw new IllegalArgumentException("No series handler registered for: " + seriesSpec.type());
                    }
                    return esPivotSeriesSpecHandler.createAggregation(seriesName, pivot, seriesSpec, this, queryContext);
                })
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Pivot pivot, SearchResponse queryResult, Aggregations aggregations, OSGeneratedQueryContext queryContext) {
        final AbsoluteRange effectiveTimerange = this.effectiveTimeRangeExtractor.extract(queryResult, query, pivot);

        final PivotResult.Builder resultBuilder = PivotResult.builder()
                .id(pivot.id())
                .effectiveTimerange(effectiveTimerange)
                .total(extractDocumentCount(queryResult));

        pivot.name().ifPresent(resultBuilder::name);

        final MultiBucketsAggregation.Bucket initialBucket = createInitialBucket(queryResult);

        retrieveBuckets(pivot.rowGroups(), initialBucket)
                .forEach(tuple -> {
                    final ImmutableList<String> keys = tuple.keys();
                    final MultiBucketsAggregation.Bucket bucket = tuple.bucket();
                    final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder()
                            .key(keys)
                            .source("leaf");
                    if (pivot.columnGroups().isEmpty() || pivot.rollup()) {
                        processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), bucket, true, "row-leaf");
                    }
                    if (!pivot.columnGroups().isEmpty()){
                        retrieveBuckets(pivot.columnGroups(), bucket)
                                .forEach(columnBucketTuple -> {
                                    final ImmutableList<String> columnKeys = columnBucketTuple.keys();
                                    final MultiBucketsAggregation.Bucket columnBucket = columnBucketTuple.bucket();

                                    processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(columnKeys), columnBucket, false, "col-leaf");
                                });
                    }
                    resultBuilder.addRow(rowBuilder.build());
                });

        if (!pivot.rowGroups().isEmpty() && pivot.rollup()) {
            final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.of());
            processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), initialBucket, true, "row-inner");
            resultBuilder.addRow(rowBuilder.source("non-leaf").build());
        }

        return resultBuilder.build();
    }

    private Stream<PivotBucket> retrieveBuckets(List<BucketSpec> pivots, MultiBucketsAggregation.Bucket initialBucket) {
        Stream<PivotBucket> result = Stream.of(PivotBucket.create(ImmutableList.of(), initialBucket));

        for (Tuple2<String, List<BucketSpec>> group : groupByConsecutiveType(pivots)) {
            result = result.flatMap((tuple) -> {
                final OSPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> bucketHandler = bucketHandlers.get(group.v1());
                return bucketHandler.extractBuckets(group.v2(), tuple);
            });
        }

        return result;
    }

    private MultiBucketsAggregation.Bucket createInitialBucket(SearchResponse queryResult) {
        return InitialBucket.create(queryResult);
    }

    private void processSeries(PivotResult.Row.Builder rowBuilder,
                               SearchResponse searchResult,
                               OSGeneratedQueryContext queryContext,
                               Pivot pivot,
                               ArrayDeque<String> columnKeys,
                               HasAggregations aggregation,
                               boolean rollup,
                               String source) {
        pivot.series().forEach(seriesSpec -> {
            final OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> seriesHandler = seriesHandlers.get(seriesSpec.type());
            final Aggregation series = seriesHandler.extractAggregationFromResult(pivot, seriesSpec, aggregation, queryContext);
            seriesHandler.handleResult(pivot, seriesSpec, searchResult, series, this, queryContext)
                    .map(value -> {
                        columnKeys.addLast(value.id());
                        final PivotResult.Value v = PivotResult.Value.create(columnKeys, value.value(), rollup, source);
                        columnKeys.removeLast();
                        return v;
                    })
                    .forEach(rowBuilder::addValue);
        });
    }

    private long extractDocumentCount(SearchResponse queryResult) {
        return queryResult.getHits().getTotalHits().value;
    }
}
