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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import com.google.common.collect.ImmutableList;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.HasAggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.MinAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ESPivot implements ESSearchTypeHandler<Pivot> {
    private static final Logger LOG = LoggerFactory.getLogger(ESPivot.class);
    private static final String AGG_NAME = "agg";

    private final Map<String, ESPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers;
    private final Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;
    private final EffectiveTimeRangeExtractor effectiveTimeRangeExtractor;

    @Inject
    public ESPivot(Map<String, ESPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers,
                   Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers,
                   EffectiveTimeRangeExtractor effectiveTimeRangeExtractor) {
        this.bucketHandlers = bucketHandlers;
        this.seriesHandlers = seriesHandlers;
        this.effectiveTimeRangeExtractor = effectiveTimeRangeExtractor;
    }

    @Override
    public void doGenerateQueryPart(Query query, Pivot pivot, ESGeneratedQueryContext queryContext) {
        LOG.debug("Generating aggregation for {}", pivot);
        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(pivot);

        final Map<Object, Object> contextMap = queryContext.contextMap();
        final AggTypes aggTypes = new AggTypes();
        contextMap.put(pivot.id(), aggTypes);

        // add global rollup series if those were requested
        if (pivot.rollup() || (pivot.rowGroups().isEmpty() && pivot.columnGroups().isEmpty())) {
            seriesStream(pivot, queryContext, "global rollup")
                    .forEach(searchSourceBuilder::aggregation);
        }

        final BucketSpecHandler.CreatedAggregations<AggregationBuilder> createdAggregations = createPivots(BucketSpecHandler.Direction.Row, query, pivot, pivot.rowGroups(), queryContext);
        final AggregationBuilder rootAggregation = createdAggregations.root();
        final AggregationBuilder leafAggregation = createdAggregations.leaf();
        final List<AggregationBuilder> metrics = createdAggregations.metrics();
        seriesStream(pivot, queryContext, "metrics")
                .forEach(aggregation -> metrics.forEach(metric -> metric.subAggregation(aggregation)));

        if (!pivot.columnGroups().isEmpty()) {
            final BucketSpecHandler.CreatedAggregations<AggregationBuilder> columnsAggregation = createPivots(BucketSpecHandler.Direction.Column, query, pivot, pivot.columnGroups(), queryContext);
            final AggregationBuilder columnsRootAggregation = columnsAggregation.root();
            final List<AggregationBuilder> columnMetrics = columnsAggregation.metrics();
            seriesStream(pivot, queryContext, "metrics")
                    .forEach(aggregation -> columnMetrics.forEach(metric -> metric.subAggregation(aggregation)));
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

    private BucketSpecHandler.CreatedAggregations<AggregationBuilder> createPivots(BucketSpecHandler.Direction direction, Query query, Pivot pivot, List<BucketSpec> pivots, ESGeneratedQueryContext queryContext) {
        AggregationBuilder leaf = null;
        AggregationBuilder root = null;
        final List<AggregationBuilder> metrics = new ArrayList<>();
        for (BucketSpec bucketSpec : pivots) {
            final ESPivotBucketSpecHandler<? extends BucketSpec> bucketHandler = bucketHandlers.get(bucketSpec.type());
            final BucketSpecHandler.CreatedAggregations<AggregationBuilder> bucketAggregations = bucketHandler.createAggregation(direction, AGG_NAME, pivot, bucketSpec, queryContext, query);
            final AggregationBuilder aggregationRoot = bucketAggregations.root();
            final AggregationBuilder aggregationLeaf = bucketAggregations.leaf();
            final List<AggregationBuilder> aggregationMetrics = bucketAggregations.metrics();

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


    private Stream<AggregationBuilder> seriesStream(Pivot pivot, ESGeneratedQueryContext queryContext, String reason) {
        return pivot.series()
                .stream()
                .distinct()
                .map((seriesSpec) -> {
                    final String seriesName = queryContext.seriesName(seriesSpec, pivot);
                    LOG.debug("Adding {} series '{}' with name '{}'", reason, seriesSpec.type(), seriesName);
                    final ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> esPivotSeriesSpecHandler = seriesHandlers.get(seriesSpec.type());
                    if (esPivotSeriesSpecHandler == null) {
                        throw new IllegalArgumentException("No series handler registered for: " + seriesSpec.type());
                    }
                    return esPivotSeriesSpecHandler.createAggregation(seriesName, pivot, seriesSpec, this, queryContext);
                })
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @WithSpan
    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Pivot pivot, SearchResponse queryResult, Aggregations aggregations, ESGeneratedQueryContext queryContext) {
        final AbsoluteRange effectiveTimerange = this.effectiveTimeRangeExtractor.extract(queryResult, query, pivot);

        final PivotResult.Builder resultBuilder = PivotResult.builder()
                .id(pivot.id())
                .effectiveTimerange(effectiveTimerange)
                .total(extractDocumentCount(queryResult));

        pivot.name().ifPresent(resultBuilder::name);

        final MultiBucketsAggregation.Bucket initialBucket = createInitialBucket(queryResult);

        retrieveBuckets(pivot, pivot.rowGroups(), initialBucket)
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
                        retrieveBuckets(pivot, pivot.columnGroups(), bucket)
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

    private Stream<PivotBucket> retrieveBuckets(Pivot pivot, List<BucketSpec> pivots, MultiBucketsAggregation.Bucket initialBucket) {
        Stream<PivotBucket> result = Stream.of(PivotBucket.create(ImmutableList.of(), initialBucket, false));

        for (BucketSpec bucketSpec : pivots) {
            result = result.flatMap((tuple) -> {
                final ESPivotBucketSpecHandler<? extends BucketSpec> bucketHandler = bucketHandlers.get(bucketSpec.type());

                return bucketHandler.extractBuckets(pivot, bucketSpec, tuple);
            });
        }

        return result;
    }

    private MultiBucketsAggregation.Bucket createInitialBucket(SearchResponse queryResult) {
        return InitialBucket.create(queryResult);
    }

    private void processSeries(PivotResult.Row.Builder rowBuilder,
                               SearchResponse searchResult,
                               ESGeneratedQueryContext queryContext,
                               Pivot pivot,
                               ArrayDeque<String> columnKeys,
                               HasAggregations aggregation,
                               boolean rollup,
                               String source) {
        pivot.series().forEach(seriesSpec -> {
            final ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> seriesHandler = this.seriesHandlers.get(seriesSpec.type());
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
