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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import one.util.streamex.EntryStream;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.aggregations.MissingBucketConstants;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.missing.Missing;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.missing.MissingAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Max;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Min;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class OSPivot implements OSSearchTypeHandler<Pivot> {
    private static final Logger LOG = LoggerFactory.getLogger(OSPivot.class);

    private final Map<String, OSPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers;
    private final Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;
    private static final TimeRange ALL_MESSAGES_TIMERANGE = allMessagesTimeRange();

    private static TimeRange allMessagesTimeRange() {
        try {
            return RelativeRange.create(0);
        } catch (InvalidRangeParametersException e) {
            LOG.error("Unable to instantiate all messages timerange: ", e);
        }
        return null;
    }

    @Inject
    public OSPivot(Map<String, OSPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers,
                   Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers) {
        this.bucketHandlers = bucketHandlers;
        this.seriesHandlers = seriesHandlers;
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

        final List<AggregationBuilder> rootBucketAggregations = doGenerateBucketAggregationsTree(query, pivot, queryContext);
        if (!rootBucketAggregations.isEmpty()) {
            rootBucketAggregations.forEach(searchSourceBuilder::aggregation);
        } else {
            LOG.debug("No aggregations generated for {}", pivot);
        }

        final MinAggregationBuilder startTimestamp = AggregationBuilders.min("timestamp-min").field("timestamp");
        final MaxAggregationBuilder endTimestamp = AggregationBuilders.max("timestamp-max").field("timestamp");
        searchSourceBuilder.aggregation(startTimestamp);
        searchSourceBuilder.aggregation(endTimestamp);
    }

    private List<AggregationBuilder> doGenerateBucketAggregationsTree(Query query,
                                                                      Pivot pivot,
                                                                      OSGeneratedQueryContext queryContext) {

        //ordered from low-level to high-level aggregations
        Deque<AggregationBuilder> bucketAggregationChain = new LinkedList<>();

        final Iterator<BucketSpec> rowBuckets = pivot.rowGroups().iterator();
        while (rowBuckets.hasNext()) {
            final BucketSpec bucketSpec = rowBuckets.next();
            final boolean isLastRowBucket = !rowBuckets.hasNext();
            final Optional<AggregationBuilder> generateSingleBucketAggregation = doGenerateSingleBucketAggregation(bucketSpec, isLastRowBucket, "row", query, pivot, queryContext);
            generateSingleBucketAggregation.ifPresent(bucketAggregationChain::addFirst);
        }

        final Iterator<BucketSpec> columnBuckets = pivot.columnGroups().iterator();
        while (columnBuckets.hasNext()) {
            final BucketSpec bucketSpec = columnBuckets.next();
            final boolean isLastColumnBucket = !columnBuckets.hasNext();
            final Optional<AggregationBuilder> generateSingleBucketAggregation = doGenerateSingleBucketAggregation(bucketSpec, isLastColumnBucket, "column", query, pivot, queryContext);
            generateSingleBucketAggregation.ifPresent(bucketAggregationChain::addFirst);
        }

        final Optional<AggregationBuilder> aggregationBuilder = bucketAggregationChain.stream()
                .reduce((aggrLower, aggrHigher) -> {
                    aggrHigher.subAggregation(aggrLower);
                    createMissingAggregation(aggrLower).map(aggrHigher::subAggregation);
                    return aggrHigher;
                });

        List<AggregationBuilder> result = new ArrayList<>();
        if (aggregationBuilder.isPresent()) {
            result.add(aggregationBuilder.get());
            createMissingAggregation(aggregationBuilder.get()).ifPresent(result::add);
        }
        return result;
    }

    private Optional<MissingAggregationBuilder> createMissingAggregation(final AggregationBuilder aggregation) {
        if (aggregation instanceof ValuesSourceAggregationBuilder) {
            final MissingAggregationBuilder missingAggregationBuilder = new MissingAggregationBuilder(MissingBucketConstants.MISSING_AGGREGATION_NAME)
                    .field(((ValuesSourceAggregationBuilder<?>) aggregation).field());
            aggregation.getSubAggregations().forEach(missingAggregationBuilder::subAggregation);
            return Optional.of(missingAggregationBuilder);
        }
        return Optional.empty();
    }

    private Optional<AggregationBuilder> doGenerateSingleBucketAggregation(BucketSpec bucketSpec,
                                                                           final boolean isLast,
                                                                           final String reason,
                                                                           final Query query,
                                                                           Pivot pivot,
                                                                           OSGeneratedQueryContext queryContext
    ) {
        final String name = queryContext.nextName();
        LOG.debug("Creating " + reason + " group aggregation '{}' as {}", bucketSpec.type(), name);
        final OSPivotBucketSpecHandler<? extends PivotSpec, ? extends Aggregation> handler = bucketHandlers.get(bucketSpec.type());
        if (handler == null) {
            throw new IllegalArgumentException("Unknown " + reason + "_group type " + bucketSpec.type());
        }
        final Optional<AggregationBuilder> generatedAggregation = handler.createAggregation(name, pivot, bucketSpec, queryContext, query);
        if (generatedAggregation.isPresent()) {
            final AggregationBuilder aggregationBuilder = generatedAggregation.get();
            // always insert the series for the final row/column group, or for each one if explicit rollup was requested
            if (isLast || pivot.rollup()) {
                seriesStream(pivot, queryContext, isLast ? "leaf " + reason : reason + " rollup")
                        .forEach(aggregationBuilder::subAggregation);
            }
        }
        return generatedAggregation;
    }

    private Stream<AggregationBuilder> seriesStream(Pivot pivot, OSGeneratedQueryContext queryContext, String reason) {
        return EntryStream.of(pivot.series())
                .mapKeyValue((integer, seriesSpec) -> {
                    final String seriesName = queryContext.seriesName(seriesSpec, pivot);
                    LOG.debug("Adding {} series '{}' with name '{}'", reason, seriesSpec.type(), seriesName);
                    final OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> OSPivotSeriesSpecHandler = seriesHandlers.get(seriesSpec.type());
                    if (OSPivotSeriesSpecHandler == null) {
                        throw new IllegalArgumentException("No series handler registered for: " + seriesSpec.type());
                    }
                    return OSPivotSeriesSpecHandler.createAggregation(seriesName, pivot, seriesSpec, this, queryContext);
                })
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private boolean isAllMessagesTimeRange(TimeRange timeRange) {
        return ALL_MESSAGES_TIMERANGE.equals(timeRange);
    }

    private AbsoluteRange extractEffectiveTimeRange(SearchResponse queryResult, Query query, Pivot pivot) {
        if (queryResult.getHits().getTotalHits().value != 0) {
            return getAbsoluteRangeFromAggregations(queryResult, query, pivot);
        } else {
            return getAbsoluteRangeFromPivot(query, pivot);
        }
    }

    private AbsoluteRange getAbsoluteRangeFromPivot(final Query query, final Pivot pivot) {
        final TimeRange pivotRange = query.effectiveTimeRange(pivot);
        return AbsoluteRange.create(pivotRange.getFrom(), pivotRange.getTo());
    }

    private AbsoluteRange getAbsoluteRangeFromAggregations(final SearchResponse queryResult, final Query query, final Pivot pivot) {
        final Min min = queryResult.getAggregations().get("timestamp-min");
        final Double from = min.getValue();
        final Max max = queryResult.getAggregations().get("timestamp-max");
        final Double to = max.getValue();
        final TimeRange pivotRange = query.effectiveTimeRange(pivot);
        return AbsoluteRange.create(
                isAllMessagesTimeRange(pivotRange) && from != 0
                        ? new DateTime(from.longValue(), DateTimeZone.UTC)
                        : pivotRange.getFrom(),
                isAllMessagesTimeRange(pivotRange) && to != 0
                        ? new DateTime(to.longValue(), DateTimeZone.UTC)
                        : pivotRange.getTo()
        );
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Pivot pivot, SearchResponse queryResult, Aggregations aggregations, OSGeneratedQueryContext queryContext) {
        final AbsoluteRange effectiveTimerange = extractEffectiveTimeRange(queryResult, query, pivot);

        final PivotResult.Builder resultBuilder = PivotResult.builder()
                .id(pivot.id())
                .effectiveTimerange(effectiveTimerange)
                .total(extractDocumentCount(queryResult, pivot, queryContext));

        // pivot results are a table where cells can contain multiple "values" and not only scalars:
        // each combination of row and column groups can contain all series (if rollup is true)
        // if rollup is false, only the "leaf" components contain the series
        // in the elasticsearch result, rows and columns are simply nested aggregations (first aggregations from rows, then from columns)
        // with metric aggregations on the corresponding levels.
        // first we iterate over all row groups (whose values generate a "key array", corresponding to the nesting level)
        // once we exhaust the row groups, we descend into the columns, which get added as values to their corresponding rows
        // on each nesting level and combination we have to check for series which we also add as values to the containing row
        final HasAggregations initialResult = createInitialResult(queryResult);

        processRows(resultBuilder, queryResult, queryContext, pivot, pivot.rowGroups(), new ArrayDeque<>(), initialResult);

        return pivot.name().map(resultBuilder::name).orElse(resultBuilder).build();
    }

    private HasAggregations createInitialResult(SearchResponse queryResult) {
        return InitialBucket.create(queryResult);
    }

    private long extractDocumentCount(SearchResponse queryResult, Pivot pivot, OSGeneratedQueryContext queryContext) {
        return queryResult.getHits().getTotalHits().value;
    }

    /*
        results from elasticsearch are nested so we need to recurse into the aggregation tree, but our result is a table, thus we need
        to keep track of the current row keys manually
         */
    private void processRows(PivotResult.Builder resultBuilder,
                             SearchResponse searchResult,
                             OSGeneratedQueryContext queryContext,
                             Pivot pivot,
                             List<BucketSpec> remainingRows,
                             ArrayDeque<String> rowKeys,
                             HasAggregations aggregation) {
        if (remainingRows.isEmpty()) {
            // this is the last row group, so we need to fork into the columns if they exist.
            // being here also means that `rowKeys` contains the maximum number of parts, one for each combination of row bucket keys
            // we will always add the series for this bucket, because that's the entire point of row groups

            final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.copyOf(rowKeys));
            // do the same for columns as we did for the rows
            processColumns(rowBuilder, searchResult, queryContext, pivot, pivot.columnGroups(), new ArrayDeque<>(), aggregation);

            // also add the series for the entire row
            // columnKeys is empty, because this is a rollup per row bucket, thus for all columns in that bucket (IOW it's not a leaf!)
            if (pivot.rollup()) {
                processSeries(rowBuilder, searchResult, queryContext, pivot, new ArrayDeque<>(), aggregation, true, "row-leaf");
            }
            resultBuilder.addRow(rowBuilder.source("leaf").build());
        } else {
            // this is not a leaf for the rows, so we add its key to the rowKeys and descend into the aggregation tree
            // afterwards we'll check if we need to add rollup for intermediate buckets. not all clients need them so they can request
            // to not calculate them
            final BucketSpec currentBucket = remainingRows.get(0);

            // this handler should never be missing, because we used it above to generate the query
            // if it is missing for some weird reason, it's ok to fail hard here
            final OSPivotBucketSpecHandler<? extends PivotSpec, ? extends Aggregation> handler = bucketHandlers.get(currentBucket.type());
            final Aggregation aggregationResult = handler.extractAggregationFromResult(pivot, currentBucket, aggregation, queryContext);
            final Stream<OSPivotBucketSpecHandler.Bucket> bucketStream = handler.handleResult(currentBucket, aggregationResult);
            // for each bucket, recurse and eventually collect all the row keys. once we reach a leaf, we'll end up in the other if branch above
            bucketStream.forEach(bucket -> {
                // push the bucket's key and use its aggregation as the new source for sub-aggregations
                rowKeys.addLast(bucket.key());
                processRows(resultBuilder, searchResult, queryContext, pivot, tail(remainingRows), rowKeys, bucket.aggregation());
                rowKeys.removeLast();
            });
            final Missing missingAggregation = aggregation.getAggregations().get(MissingBucketConstants.MISSING_AGGREGATION_NAME);
            if (missingAggregation != null && missingAggregation.getDocCount() > 0) {
                rowKeys.addLast(MissingBucketConstants.MISSING_BUCKET_NAME);
                processRows(resultBuilder, searchResult, queryContext, pivot, tail(remainingRows), rowKeys, missingAggregation);
                rowKeys.removeLast();
            }
            // also add the series for this row key if the client wants rollups
            if (pivot.rollup()) {
                final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.copyOf(rowKeys));
                // columnKeys is empty, because this is a rollup per row bucket, thus for all columns in that bucket (IOW it's not a leaf!)
                processSeries(rowBuilder, searchResult, queryContext, pivot, new ArrayDeque<>(), aggregation, true, "row-inner");
                resultBuilder.addRow(rowBuilder.source("non-leaf").build());
            }

        }
    }

    private void processColumns(PivotResult.Row.Builder rowBuilder,
                                SearchResponse searchResult,
                                OSGeneratedQueryContext queryContext,
                                Pivot pivot,
                                List<BucketSpec> remainingColumns,
                                ArrayDeque<String> columnKeys,
                                HasAggregations aggregation) {
        if (remainingColumns.isEmpty()) {
            // this is the leaf cell of the pivot table, simply add all the series for the complete column key array
            // in the rollup: false case, this is the only set of series that is going to be added to the result
            // if we simply don't have any column groups, then don't bother adding the series, this is the special case that
            // only row grouping was requested, and the rollup for rows is automatically added anyway. otherwise we'll end up
            // with duplicate data entries
            if (!columnKeys.isEmpty()) {
                processSeries(rowBuilder, searchResult, queryContext, pivot, columnKeys, aggregation, false, "col-leaf");
            }
        } else {
            // for a non-leaf column group, we need to recurse further into the aggregation tree
            // and if rollup was requested we'll add intermediate series according to the column keys
            final BucketSpec currentBucket = remainingColumns.get(0);

            // this handler should never be missing, because we used it above to generate the query
            // if it is missing for some weird reason, it's ok to fail hard here
            final OSPivotBucketSpecHandler<? extends PivotSpec, ? extends Aggregation> handler = bucketHandlers.get(currentBucket.type());
            final Aggregation aggregationResult = handler.extractAggregationFromResult(pivot, currentBucket, aggregation, queryContext);
            final Stream<OSPivotBucketSpecHandler.Bucket> bucketStream = handler.handleResult(currentBucket, aggregationResult);

            // for each bucket, recurse and eventually collect all the column keys. once we reach a leaf, we'll end up in the other if branch above
            bucketStream.forEach(bucket -> {
                // push the bucket's key and use its aggregation as the new source for sub-aggregations
                columnKeys.addLast(bucket.key());
                processColumns(rowBuilder, searchResult, queryContext, pivot, tail(remainingColumns), columnKeys, bucket.aggregation());
                columnKeys.removeLast();
            });
            final Missing missingAggregation = aggregation.getAggregations().get(MissingBucketConstants.MISSING_AGGREGATION_NAME);
            if (missingAggregation != null && missingAggregation.getDocCount() > 0) {
                columnKeys.addLast(MissingBucketConstants.MISSING_BUCKET_NAME);
                processColumns(rowBuilder, searchResult, queryContext, pivot, tail(remainingColumns), columnKeys, missingAggregation);
                columnKeys.removeLast();
            }
            // also add the series for the base column key if the client wants rollups, the complete column key is processed in the leaf branch
            // don't add the empty column key rollup, because that's not the correct bucket here, it's being done in the row-leaf code
            if (pivot.rollup() && !columnKeys.isEmpty()) {
                // columnKeys is not empty, because this is a rollup per column in a row
                processSeries(rowBuilder, searchResult, queryContext, pivot, columnKeys, aggregation, true, "col-inner");
            }

        }
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

    private static <T> List<T> tail(List<T> list) {
        Preconditions.checkArgument(!list.isEmpty(), "List must not be empty!");
        return list.subList(1, list.size());
    }

    /**
     * This solely exists to hide the nasty type signature of the aggregation type map from the rest of the code.
     * It's just ugly and in the way.
     */
    public static class AggTypes {
        final IdentityHashMap<PivotSpec, Tuple2<String, Class<? extends Aggregation>>> aggTypeMap = new IdentityHashMap<>();

        public void record(PivotSpec pivotSpec, String name, Class<? extends Aggregation> aggClass) {
            aggTypeMap.put(pivotSpec, Tuple.tuple(name, aggClass));
        }

        public Aggregation getSubAggregation(PivotSpec pivotSpec, HasAggregations currentAggregationOrBucket) {
            final Tuple2<String, Class<? extends Aggregation>> tuple2 = getTypes(pivotSpec);
            return currentAggregationOrBucket.getAggregations().get(tuple2.v1);
        }

        public Tuple2<String, Class<? extends Aggregation>> getTypes(PivotSpec pivotSpec) {
            return aggTypeMap.get(pivotSpec);
        }
    }
}
