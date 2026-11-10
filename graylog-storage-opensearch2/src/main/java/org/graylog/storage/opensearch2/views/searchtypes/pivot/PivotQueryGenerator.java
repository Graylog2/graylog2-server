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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
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

    private final Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers;
    private final Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;

    PivotQueryGenerator(Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers,
                        Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers) {
        this.bucketHandlers = bucketHandlers;
        this.seriesHandlers = seriesHandlers;
    }

    void generate(Query query, Pivot pivot, OSGeneratedQueryContext queryContext) {
        LOG.debug("Generating aggregation for {}", pivot);
        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(pivot);

        final boolean generateRollups = pivot.rollup() || (pivot.rowGroups().isEmpty() && pivot.columnGroups().isEmpty());

        if (generateRollups) {
            addGlobalRollupSeries(pivot, queryContext, searchSourceBuilder);
        }

        final BucketSpecHandler.CreatedAggregations<AggregationBuilder> rowAggregations =
                createPivots(BucketSpecHandler.Direction.Row, query, pivot, pivot.rowGroups(), queryContext);
        addRowSeries(pivot, queryContext, searchSourceBuilder, rowAggregations, generateRollups);

        if (!pivot.columnGroups().isEmpty()) {
            addColumnGroups(query, pivot, queryContext, searchSourceBuilder, rowAggregations.leaf());
        }

        if (rowAggregations.root() != null) {
            searchSourceBuilder.aggregation(rowAggregations.root());
        }

        addTimestampAggregations(searchSourceBuilder);
    }

    private void addGlobalRollupSeries(Pivot pivot, OSGeneratedQueryContext queryContext, SearchSourceBuilder searchSourceBuilder) {
        seriesStream(pivot, queryContext, "global rollup")
                .filter(result -> Placement.METRIC.equals(result.placement()))
                .map(SeriesAggregationBuilder::aggregationBuilder)
                .forEach(searchSourceBuilder::aggregation);
    }

    private void addRowSeries(Pivot pivot,
                              OSGeneratedQueryContext queryContext,
                              SearchSourceBuilder searchSourceBuilder,
                              BucketSpecHandler.CreatedAggregations<AggregationBuilder> rowAggregations,
                              boolean generateRollups) {
        final AggregationBuilder rootAggregation = rowAggregations.root();
        final List<AggregationBuilder> metrics = rowAggregations.metrics();
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
                                 SearchSourceBuilder searchSourceBuilder,
                                 AggregationBuilder rowLeafAggregation) {
        final BucketSpecHandler.CreatedAggregations<AggregationBuilder> columnsAggregation =
                createPivots(BucketSpecHandler.Direction.Column, query, pivot, pivot.columnGroups(), queryContext);
        final List<AggregationBuilder> columnMetrics = columnsAggregation.metrics();
        seriesStream(pivot, queryContext, "metrics")
                .forEach(result -> {
                    final var aggregationBuilder = result.aggregationBuilder();
                    switch (result.placement()) {
                        case COLUMN -> columnsAggregation.leaf().subAggregation(aggregationBuilder);
                        case METRIC -> columnMetrics.forEach(metric -> metric.subAggregation(aggregationBuilder));
                    }
                });
        if (rowLeafAggregation != null) {
            rowLeafAggregation.subAggregation(columnsAggregation.root());
        } else {
            searchSourceBuilder.aggregation(columnsAggregation.root());
        }
    }

    private void addTimestampAggregations(SearchSourceBuilder searchSourceBuilder) {
        final MinAggregationBuilder startTimestamp = AggregationBuilders.min("timestamp-min").field("timestamp");
        final MaxAggregationBuilder endTimestamp = AggregationBuilders.max("timestamp-max").field("timestamp");
        searchSourceBuilder.aggregation(startTimestamp);
        searchSourceBuilder.aggregation(endTimestamp);
    }

    private BucketSpecHandler.CreatedAggregations<AggregationBuilder> createPivots(BucketSpecHandler.Direction direction, Query query, Pivot pivot, List<BucketSpec> pivots, OSGeneratedQueryContext queryContext) {
        AggregationBuilder leaf = null;
        AggregationBuilder root = null;
        final List<AggregationBuilder> metrics = new ArrayList<>();
        for (BucketSpec bucketSpec : pivots) {
            final OSPivotBucketSpecHandler<? extends BucketSpec> bucketHandler = bucketHandlers.get(bucketSpec.type());
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

    private Stream<SeriesAggregationBuilder> seriesStream(Pivot pivot, OSGeneratedQueryContext queryContext, String reason) {
        return pivot.series()
                .stream()
                .distinct()
                .flatMap((seriesSpec) -> {
                    final String seriesName = queryContext.seriesName(seriesSpec, pivot);
                    LOG.debug("Adding {} series '{}' with name '{}'", reason, seriesSpec.type(), seriesName);
                    final OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> esPivotSeriesSpecHandler = seriesHandlers.get(seriesSpec.type());
                    if (esPivotSeriesSpecHandler == null) {
                        throw new IllegalArgumentException("No series handler registered for: " + seriesSpec.type());
                    }
                    return esPivotSeriesSpecHandler.createAggregation(seriesName, pivot, seriesSpec, queryContext).stream();
                });
    }
}
