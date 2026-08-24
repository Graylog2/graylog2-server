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
import org.graylog.plugins.views.search.aggregations.OtherBucketDerivation;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.buckets.OSValuesHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.aggregations.SingleMetricAggregateBase;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog.plugins.views.search.aggregations.OtherBucketConstants.OTHER_BUCKET_NAME;

/**
 * Turns an OpenSearch {@link MultiSearchItem} response into a {@link PivotResult} by walking the row and column bucket
 * aggregations, extracting series values for each leaf, and adding the optional rollup row.
 */
class PivotResultProcessor {
    /** The descent key every bucket handler hard-codes when reading its own child aggregation back. */
    private static final String AGG_NAME = "agg";

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

        retrieveBuckets(pivot, pivot.rowGroups(), initialBucket, queryContext)
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

        if (tuple.isOtherBucket()) {
            // The (Other) bucket is synthetic: it has no aggregations to read, only values derived from its siblings.
            addDerivedValues(rowBuilder, pivot, tuple.derivedValues(), new ArrayDeque<>(), true, "row-leaf");
            if (!pivot.columnGroups().isEmpty()) {
                addOtherColumnValues(rowBuilder, pivot, queryContext, colGroupNames, seriesNames, tuple);
            }
            return rowBuilder.build();
        }

        if (pivot.columnGroups().isEmpty() || pivot.rollup()) {
            processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), rowBucket, true, "row-leaf");
        }
        if (!pivot.columnGroups().isEmpty()) {
            final var contextWithRowBucket = queryContext.withRowBucket(rowBucket);
            retrieveBuckets(pivot, pivot.columnGroups(), rowBucket, queryContext)
                    .forEach(columnBucketTuple -> {
                        final ImmutableList<String> columnKeys = columnBucketTuple.keys();
                        colGroupNames.add(String.join(", ", Stream.concat(columnKeys.stream(), seriesNames.stream()).toList()));

                        final MultiBucketBase columnBucket = columnBucketTuple.bucket();

                        if (columnBucketTuple.isOtherBucket()) {
                            addDerivedValues(rowBuilder, pivot, columnBucketTuple.derivedValues(), new ArrayDeque<>(columnKeys), false, "col-leaf");
                        } else {
                            processSeries(rowBuilder, queryResult, contextWithRowBucket, pivot, new ArrayDeque<>(columnKeys), columnBucket, false, "col-leaf");
                        }
                    });
        }
        return rowBuilder.build();
    }

    private void addDerivedValues(PivotResult.Row.Builder rowBuilder,
                                  Pivot pivot,
                                  Map<String, Object> derivedValues,
                                  ArrayDeque<String> columnKeys,
                                  boolean rollup,
                                  String source) {
        pivot.series().forEach(seriesSpec -> {
            final Object value = derivedValues.get(seriesSpec.id());
            if (value == null) {
                // Not derivable for the (Other) bucket - omitted rather than reported as zero.
                return;
            }
            columnKeys.addLast(seriesSpec.id());
            rowBuilder.addValue(PivotResult.Value.create(columnKeys, value, rollup, source));
            columnKeys.removeLast();
        });
    }

    /**
     * Fills in the {@code (Other)} row's per-column cells: {@code parentColumn - sum(shownRowsColumn)} for each
     * column key that appears under the row grouping's own copy of the column tree (see
     * {@link PivotQueryGenerator#OTHER_BUCKET_COLUMNS_AGG_NAME}). Also registers those column keys with
     * {@code colGroupNames}, since a column may only ever be populated here when every row that would otherwise
     * have surfaced it fell into the tail.
     */
    private void addOtherColumnValues(PivotResult.Row.Builder rowBuilder,
                                      Pivot pivot,
                                      OSGeneratedQueryContext queryContext,
                                      List<String> colGroupNames,
                                      List<String> seriesNames,
                                      PivotBucket tuple) {
        final MultiBucketBase parent = tuple.otherParent();
        if (parent == null) {
            return;
        }
        final MultiBucketBase parentColumnTreeRoot = remapOtherBucketColumnTree(parent);
        if (parentColumnTreeRoot == null) {
            return;
        }

        final Map<ImmutableList<String>, MultiBucketBase> parentColumns =
                retrieveBuckets(pivot, pivot.columnGroups(), parentColumnTreeRoot, queryContext)
                        .filter(bucket -> !bucket.isOtherBucket())
                        .collect(Collectors.toMap(PivotBucket::keys, PivotBucket::bucket, (first, second) -> first, LinkedHashMap::new));

        final List<MultiBucketBase> siblings = tuple.otherSiblings();
        // Each shown sibling is itself a terms bucket - one of the two targets the second column tree is attached
        // to (see PivotQueryGenerator#addColumnGroups) - so it needs the same re-key as the parent before its own
        // copy of the tree can be walked generically.
        final List<Map<ImmutableList<String>, MultiBucketBase>> shownColumnsPerRow = siblings.stream()
                .map(shown -> {
                    final MultiBucketBase columnTreeRoot = remapOtherBucketColumnTree(shown);
                    if (columnTreeRoot == null) {
                        return Map.<ImmutableList<String>, MultiBucketBase>of();
                    }
                    return retrieveBuckets(pivot, pivot.columnGroups(), columnTreeRoot, queryContext)
                            .filter(bucket -> !bucket.isOtherBucket())
                            .collect(Collectors.toMap(PivotBucket::keys, PivotBucket::bucket, (first, second) -> first, LinkedHashMap::new));
                })
                .toList();

        // A sibling's own column breakdown can be truncated by that grouping's limit too: if the doc counts of its
        // reported column buckets fall short of its own total, some of its documents live in a column-level tail
        // this walk cannot see. A column key absent from such a sibling cannot be assumed to contribute zero - a
        // missing remap (columnTreeRoot == null above) also lands here, since its accounted-for total is 0.
        final List<Boolean> siblingColumnsTruncated = new ArrayList<>();
        for (int i = 0; i < siblings.size(); i++) {
            final long accountedFor = shownColumnsPerRow.get(i).values().stream().mapToLong(MultiBucketBase::docCount).sum();
            siblingColumnsTruncated.add(accountedFor < siblings.get(i).docCount());
        }

        parentColumns.forEach((columnKeys, parentColumnBucket) -> {
            final List<MultiBucketBase> shownColumnBuckets = new ArrayList<>();
            for (int i = 0; i < shownColumnsPerRow.size(); i++) {
                final MultiBucketBase shownColumn = shownColumnsPerRow.get(i).get(columnKeys);
                if (shownColumn != null) {
                    shownColumnBuckets.add(shownColumn);
                } else if (siblingColumnsTruncated.get(i)) {
                    // At least one sibling's contribution to this column is unknown - omit the whole cell rather
                    // than under-subtract it.
                    return;
                }
            }

            final Map<String, Object> columnValues = new LinkedHashMap<>();
            pivot.series().stream()
                    .filter(OtherBucketDerivation::isDerivable)
                    .forEach(seriesSpec -> deriveColumnValue(pivot, queryContext, seriesSpec, parentColumnBucket, shownColumnBuckets)
                            .ifPresent(value -> columnValues.put(seriesSpec.id(), value)));

            if (!columnValues.isEmpty()) {
                colGroupNames.add(String.join(", ", Stream.concat(columnKeys.stream(), seriesNames.stream()).toList()));
                addDerivedValues(rowBuilder, pivot, columnValues, new ArrayDeque<>(columnKeys), false, "col-leaf");
            }
        });
    }

    /**
     * The second column tree is attached under {@link PivotQueryGenerator#OTHER_BUCKET_COLUMNS_AGG_NAME} rather
     * than {@link #AGG_NAME}, because the row grouping's own terms aggregation already occupies that name on the
     * same bucket. Bucket handlers hard-code {@link #AGG_NAME} as their descent key, so re-key the single entry we
     * care about before handing it to {@link #retrieveBuckets}.
     */
    private MultiBucketBase remapOtherBucketColumnTree(MultiBucketBase bucket) {
        final Aggregate columnTree = bucket.aggregations().get(PivotQueryGenerator.OTHER_BUCKET_COLUMNS_AGG_NAME);
        if (columnTree == null) {
            return null;
        }
        return InitialBucket.builder()
                .docCount(bucket.docCount())
                .aggregations(Map.of(AGG_NAME, columnTree))
                .build();
    }

    /**
     * Derives one series' value for one column of the {@code (Other)} row. Field-less {@code count()} cannot use
     * {@code sum_other_doc_count} here - that number describes the row grouping's tail, not the tail within one
     * column - so it subtracts doc counts instead. Mean-based series need the same companion {@code extended_stats}
     * aggregation that {@code OSValuesHandler.addStatsCompanions} attaches at the row level, mirrored onto the
     * column tree by {@code PivotQueryGenerator#createColumnTree}.
     */
    private Optional<Object> deriveColumnValue(Pivot pivot,
                                               OSGeneratedQueryContext queryContext,
                                               SeriesSpec seriesSpec,
                                               MultiBucketBase parentColumn,
                                               List<MultiBucketBase> shownColumns) {
        if (!OtherBucketDerivation.requiresSeriesValue(seriesSpec)) {
            final List<Long> shownDocCounts = shownColumns.stream().map(MultiBucketBase::docCount).toList();
            return OtherBucketDerivation.deriveFromDocCounts(seriesSpec, parentColumn.docCount(), shownDocCounts);
        }

        if (OtherBucketDerivation.requiresStats(seriesSpec)) {
            final String statsName = OSValuesHandler.statsAggregationName(OSValuesHandler.statsFieldOf(seriesSpec));
            final OtherBucketDerivation.Stats parentStats = readStats(parentColumn.aggregations(), statsName);
            if (parentStats == null) {
                return Optional.empty();
            }
            final List<OtherBucketDerivation.Stats> shownStats = shownColumns.stream()
                    .map(bucket -> readStats(bucket.aggregations(), statsName))
                    .toList();
            if (shownStats.stream().anyMatch(Objects::isNull)) {
                return Optional.empty();
            }
            return OtherBucketDerivation.derive(seriesSpec,
                    new OtherBucketDerivation.TailInput(0L, null, List.of(), parentStats, shownStats));
        }

        final String seriesName = queryContext.seriesName(seriesSpec, pivot);
        final Double parentValue = readNumeric(parentColumn.aggregations(), seriesName);
        if (parentValue == null) {
            return Optional.empty();
        }
        final List<Double> shownValues = shownColumns.stream()
                .map(bucket -> readNumeric(bucket.aggregations(), seriesName))
                .toList();
        if (shownValues.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        return OtherBucketDerivation.derive(seriesSpec,
                new OtherBucketDerivation.TailInput(0L, parentValue, shownValues, null, List.of()));
    }

    private static Double readNumeric(Map<String, Aggregate> aggregations, String name) {
        final Aggregate aggregation = aggregations.get(name);
        if (aggregation == null) {
            return null;
        }
        if (aggregation._get() instanceof final SingleMetricAggregateBase singleValue) {
            return singleValue.value();
        }
        return null;
    }

    private static OtherBucketDerivation.Stats readStats(Map<String, Aggregate> aggregations, String name) {
        return Optional.ofNullable(aggregations.get(name))
                .filter(Aggregate::isExtendedStats)
                .map(Aggregate::extendedStats)
                .map(stats -> new OtherBucketDerivation.Stats(
                        stats.count(),
                        stats.sum(),
                        stats.sumOfSquares() == null ? 0.0d : stats.sumOfSquares()))
                .orElse(null);
    }

    private PivotResult.Row buildRollupRow(Pivot pivot,
                                           MultiSearchItem<JsonData> queryResult,
                                           OSGeneratedQueryContext queryContext,
                                           InitialBucket initialBucket) {
        final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.of());
        processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), initialBucket, true, "row-inner");
        return rowBuilder.source("non-leaf").build();
    }

    private Stream<PivotBucket> retrieveBuckets(Pivot pivot, List<BucketSpec> pivots, MultiBucketBase aggregations, OSGeneratedQueryContext queryContext) {
        Stream<PivotBucket> result = Stream.of(PivotBucket.create(ImmutableList.of(), aggregations));

        for (BucketSpec bucketSpec : pivots) {
            result = result.flatMap((tuple) -> {
                if (tuple.isOtherBucket()) {
                    // The tail cannot be broken down by deeper groupings, so pad the key instead of descending.
                    // "(Other)" in the inner position reads accurately: all remaining values, within all remaining.
                    final ImmutableList<String> paddedKeys = ImmutableList.<String>builder()
                            .addAll(tuple.keys())
                            .addAll(Collections.nCopies(bucketSpec.fields().size(), OTHER_BUCKET_NAME))
                            .build();
                    return Stream.of(PivotBucket.createOther(paddedKeys, tuple.bucket(), tuple.derivedValues(),
                            tuple.otherParent(), tuple.otherSiblings()));
                }
                final OSPivotBucketSpecHandler<? extends BucketSpec> bucketHandler = bucketHandlers.get(bucketSpec.type());
                return bucketHandler.extractBuckets(pivot, bucketSpec, tuple, queryContext);
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
