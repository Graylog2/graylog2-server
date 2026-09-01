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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.storage.opensearch3.views.MutableSearchRequestBuilder;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.buckets.OSValuesHandler;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Builds the OpenSearch aggregation tree for a {@link Pivot}: global rollup series, row and column bucket groups,
 * the metric/row/column/root series placed within them, and the timestamp range aggregations.
 */
class PivotQueryGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(PivotQueryGenerator.class);
    private static final String AGG_NAME = "agg";

    /**
     * Name for the second, independent column-aggregation tree attached to the enclosing {@code filters}
     * aggregation of every row grouping that opts into the {@code (Other)} bucket. It cannot reuse {@link #AGG_NAME}
     * because that name is already taken there by the row grouping's own terms aggregation; the {@code (Other)}-row
     * derivation in {@link PivotResultProcessor} re-keys it back to {@link #AGG_NAME} before walking it with the
     * ordinary bucket handlers, which hard-code that name as their descent key.
     */
    static final String OTHER_BUCKET_COLUMNS_AGG_NAME = "other-bucket-columns";

    private final Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers;
    private final Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec>> seriesHandlers;

    PivotQueryGenerator(Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers,
                        Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec>> seriesHandlers) {
        this.bucketHandlers = bucketHandlers;
        this.seriesHandlers = seriesHandlers;
    }

    void generate(Query query, Pivot pivot, OSGeneratedQueryContext queryContext) {
        LOG.debug("Generating aggregation for {}", pivot);
        final MutableSearchRequestBuilder searchSourceBuilder = queryContext.searchSourceBuilder(pivot);

        final boolean generateRollups = pivot.rollup() || (pivot.rowGroups().isEmpty() && pivot.columnGroups().isEmpty());

        if (generateRollups) {
            addGlobalRollupSeries(pivot, queryContext, searchSourceBuilder);
        }

        final List<List<MutableNamedAggregationBuilder>> otherBucketRowTargets = new ArrayList<>();
        final BucketSpecHandler.CreatedAggregations<MutableNamedAggregationBuilder> rowAggregations =
                createPivots(BucketSpecHandler.Direction.Row, query, pivot, pivot.rowGroups(), queryContext, AGG_NAME, otherBucketRowTargets);
        addRowSeries(pivot, queryContext, searchSourceBuilder, rowAggregations, generateRollups);

        if (!pivot.columnGroups().isEmpty()) {
            addColumnGroups(query, pivot, queryContext, searchSourceBuilder, rowAggregations.leaf(), otherBucketRowTargets);
        }

        if (rowAggregations.root() != null) {
            searchSourceBuilder.aggregation(rowAggregations.root());
        }

        addTimestampAggregations(searchSourceBuilder);
    }

    private void addGlobalRollupSeries(Pivot pivot, OSGeneratedQueryContext queryContext, MutableSearchRequestBuilder searchSourceBuilder) {
        seriesStream(pivot, queryContext, "global rollup")
                .filter(result -> Placement.METRIC.equals(result.placement()))
                .map(SeriesAggregationBuilder::aggregationBuilder)
                .forEach(searchSourceBuilder::aggregation);
    }

    private void addRowSeries(Pivot pivot,
                              OSGeneratedQueryContext queryContext,
                              MutableSearchRequestBuilder searchSourceBuilder,
                              BucketSpecHandler.CreatedAggregations<MutableNamedAggregationBuilder> rowAggregations,
                              boolean generateRollups) {
        final MutableNamedAggregationBuilder rootAggregation = rowAggregations.root();
        final List<MutableNamedAggregationBuilder> metrics = rowAggregations.metrics();
        seriesStream(pivot, queryContext, "metrics")
                .forEach(result -> {
                    switch (result.placement()) {
                        case METRIC -> metrics.forEach(metric -> metric.subAggregation(result.aggregationBuilder()));
                        case ROW -> rootAggregation.subAggregation(result.aggregationBuilder());
                        case ROOT -> {
                            if (!generateRollups) {
                                searchSourceBuilder.aggregation(result.aggregationBuilder());
                            }
                        }
                    }
                });
    }

    private void addColumnGroups(Query query,
                                 Pivot pivot,
                                 OSGeneratedQueryContext queryContext,
                                 MutableSearchRequestBuilder searchSourceBuilder,
                                 MutableNamedAggregationBuilder rowLeafAggregation,
                                 List<List<MutableNamedAggregationBuilder>> otherBucketRowTargets) {
        final BucketSpecHandler.CreatedAggregations<MutableNamedAggregationBuilder> columnsAggregation =
                createColumnTree(query, pivot, queryContext, AGG_NAME, false);
        if (rowLeafAggregation != null) {
            rowLeafAggregation.subAggregation(columnsAggregation.root());
        } else {
            searchSourceBuilder.aggregation(columnsAggregation.root());
        }

        // The (Other) row's per-column cells are derived as parentColumn - sum(shownRowsColumn). Both the parent
        // (the enclosing filters bucket) and every shown sibling (a terms bucket) must be able to read this tree,
        // so it is attached to BOTH targets of every opted-in row grouping - exactly where
        // OSValuesHandler.addStatsCompanions already places its per-field extended_stats companions. Attaching it
        // only to the filters bucket (the parent) would silently mis-read as some deeper row grouping's own tree
        // whenever the opted-in grouping isn't the innermost row level. Because these are mutable builder trees, a
        // second independent tree is built per grouping rather than re-parenting the one attached above; that one
        // instance is then shared by both targets at that grouping's level, since it is the same declarative
        // aggregation definition at both places.
        otherBucketRowTargets.forEach(targets -> {
            final MutableNamedAggregationBuilder columnTreeRoot =
                    createColumnTree(query, pivot, queryContext, OTHER_BUCKET_COLUMNS_AGG_NAME, true).root();
            targets.forEach(target -> target.subAggregation(columnTreeRoot));
        });
    }

    /**
     * @param withStatsCompanions whether to also attach one {@code extended_stats} companion per
     *                            {@link OSValuesHandler#statsFields}, needed to reconstruct mean-based series
     *                            (avg/variance/stddev/sumOfSquares) within a single column of the {@code (Other)}
     *                            row. Only the second, {@code (Other)}-row-dedicated tree needs these; the ordinary
     *                            per-row column tree does not, since ordinary rows read series values directly.
     */
    private BucketSpecHandler.CreatedAggregations<MutableNamedAggregationBuilder> createColumnTree(Query query,
                                                                                                    Pivot pivot,
                                                                                                    OSGeneratedQueryContext queryContext,
                                                                                                    String rootName,
                                                                                                    boolean withStatsCompanions) {
        final BucketSpecHandler.CreatedAggregations<MutableNamedAggregationBuilder> columnsAggregation =
                createPivots(BucketSpecHandler.Direction.Column, query, pivot, pivot.columnGroups(), queryContext, rootName, new ArrayList<>());
        final List<MutableNamedAggregationBuilder> columnMetrics = columnsAggregation.metrics();
        seriesStream(pivot, queryContext, "metrics")
                .forEach(result -> {
                    final var aggregationBuilder = result.aggregationBuilder();
                    switch (result.placement()) {
                        case COLUMN -> columnsAggregation.leaf().subAggregation(aggregationBuilder);
                        case METRIC -> columnMetrics.forEach(metric -> metric.subAggregation(aggregationBuilder));
                    }
                });
        if (withStatsCompanions) {
            OSValuesHandler.statsFields(pivot).forEach(field -> columnMetrics.forEach(metric ->
                    metric.subAggregation(new MutableNamedAggregationBuilder(
                            OSValuesHandler.statsAggregationName(field),
                            Aggregation.builder().extendedStats(e -> e.field(field))))));
        }
        return columnsAggregation;
    }

    private void addTimestampAggregations(MutableSearchRequestBuilder searchSourceBuilder) {
        final MutableNamedAggregationBuilder startTimestamp = new MutableNamedAggregationBuilder(
                "timestamp-min",
                Aggregation.builder().min(f -> f.field("timestamp")));
        final MutableNamedAggregationBuilder endTimestamp = new MutableNamedAggregationBuilder(
                "timestamp-max",
                Aggregation.builder().max(f -> f.field("timestamp")));
        searchSourceBuilder.aggregation(startTimestamp);
        searchSourceBuilder.aggregation(endTimestamp);
    }

    /**
     * @param rootName             the name given to the outermost bucket aggregation of {@code pivots}. Every
     *                             deeper level keeps using {@link #AGG_NAME}, since each is nested inside its own
     *                             private leaf and never a sibling of anything already using that name.
     * @param otherBucketRowTargets for {@code direction == Row}, collects the OWN {@code metrics()} - the terms
     *                             aggregation and the enclosing filters aggregation - of every {@link Values}
     *                             grouping with {@code otherBucket() == true}, so a second column tree can later be
     *                             attached to both. Ignored for {@code direction == Column}.
     */
    private BucketSpecHandler.CreatedAggregations<MutableNamedAggregationBuilder> createPivots(BucketSpecHandler.Direction direction,
                                                                                                Query query,
                                                                                                Pivot pivot,
                                                                                                List<BucketSpec> pivots,
                                                                                                OSGeneratedQueryContext queryContext,
                                                                                                String rootName,
                                                                                                List<List<MutableNamedAggregationBuilder>> otherBucketRowTargets) {
        MutableNamedAggregationBuilder leaf = null;
        MutableNamedAggregationBuilder root = null;
        final List<MutableNamedAggregationBuilder> metrics = new ArrayList<>();
        for (BucketSpec bucketSpec : pivots) {
            final OSPivotBucketSpecHandler<? extends BucketSpec> bucketHandler = bucketHandlers.get(bucketSpec.type());
            final String levelName = root == null ? rootName : AGG_NAME;
            final BucketSpecHandler.CreatedAggregations<MutableNamedAggregationBuilder> bucketAggregations = bucketHandler.createAggregation(direction, levelName, pivot, bucketSpec, queryContext, query);
            final MutableNamedAggregationBuilder aggregationRoot = bucketAggregations.root();
            final MutableNamedAggregationBuilder aggregationLeaf = bucketAggregations.leaf();
            final List<MutableNamedAggregationBuilder> aggregationMetrics = bucketAggregations.metrics();

            metrics.addAll(aggregationMetrics);
            if (direction == BucketSpecHandler.Direction.Row && bucketSpec instanceof Values values && values.otherBucket()) {
                otherBucketRowTargets.add(aggregationMetrics);
            }
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
}
