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
package org.graylog.storage.elasticsearch6.views.searchtypes.pivot;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import one.util.streamex.EntryStream;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.script.Script;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.HasAggregations;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.metrics.min.MinAggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESSearchTypeHandler;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ESPivot implements ESSearchTypeHandler<Pivot> {
    private static final Logger LOG = LoggerFactory.getLogger(ESPivot.class);
    private final Map<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers;
    private final Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;
    private static final TimeRange ALL_MESSAGES_TIMERANGE = allMessagesTimeRange();
    private static final String TERMS_SEPARATOR = "\u2E31";
    private static final String UNKNOWN_FIELD = "\u200B";

    private static final String ROWS_POSTFIX = "-rows";
    private static final String COLUMNS_POSTFIX = "-columns";

    private static TimeRange allMessagesTimeRange() {
        try {
            return RelativeRange.create(0);
        } catch (InvalidRangeParametersException e){
            LOG.error("Unable to instantiate all messages timerange: ", e);
        }
        return null;
    }

    @Inject
    public ESPivot(Map<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers,
                   Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers) {
        this.bucketHandlers = bucketHandlers;
        this.seriesHandlers = seriesHandlers;
    }

    private String createAggregationName(final ESGeneratedQueryContext queryContext, final Pivot pivot, final String postfix) {
        final String aggregationName = queryContext.nextName();
        queryContext.contextMap().put(pivot.id() + postfix, aggregationName);
        return aggregationName;
    }

    @Override
    public void doGenerateQueryPart(final SearchJob job, final Query query, final Pivot pivot, final ESGeneratedQueryContext queryContext) {
        LOG.debug("Generating aggregation for {}", pivot);
        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(pivot);

        final Map<Object, Object> contextMap = queryContext.contextMap();
        final AggTypes aggTypes = new AggTypes();
        contextMap.put(pivot.id(), aggTypes);

        final List<AggregationBuilder> seriesAggregations = seriesStream(pivot, queryContext, "global rollup").collect(Collectors.toList());

        // add rollup series if those were requested
        if (pivot.rollup()) seriesAggregations.forEach(searchSourceBuilder::aggregation);

        final List<Order> bucketOrder = orderListForPivot(pivot, queryContext);

        // if rows are defined, begin nesting from the rows
        if(!pivot.rowGroups().isEmpty()) {
            final AggregationBuilder rowsAggregation = createAggregation(createAggregationName(queryContext, pivot, ROWS_POSTFIX), pivot.rowGroups(), bucketOrder);

            // add rollup series if those were requested
            if (pivot.rollup()) seriesAggregations.forEach(rowsAggregation::subAggregation);

            searchSourceBuilder.aggregation(rowsAggregation);

            final AggregationBuilder columnsAggregation = createAggregation(createAggregationName(queryContext, pivot, COLUMNS_POSTFIX), pivot.columnGroups(), bucketOrder);

            seriesAggregations.forEach(columnsAggregation::subAggregation);
            rowsAggregation.subAggregation(columnsAggregation);
        } else {
            // only columns are defined. Still add an aggregation to the searchSourceBuilder, so we get 1 row
            final AggregationBuilder columnsAggregation = createAggregation(createAggregationName(queryContext, pivot, COLUMNS_POSTFIX), pivot.columnGroups(), bucketOrder);

            seriesAggregations.forEach(columnsAggregation::subAggregation);
            searchSourceBuilder.aggregation(columnsAggregation);
        }

        final MinAggregationBuilder startTimestamp = AggregationBuilders.min("timestamp-min").field("timestamp");
        final MaxAggregationBuilder endTimestamp = AggregationBuilders.max("timestamp-max").field("timestamp");

        searchSourceBuilder.aggregation(startTimestamp);
        searchSourceBuilder.aggregation(endTimestamp);
    }

    private String createScript(List<BucketSpec> pivots) {
        return pivots.stream()
                .map(BucketSpec::field)
//                .map(field -> field == null ? "'null field'" : "(doc.containsKey('" + field + "') ? doc['" + field + "'].value : 'unknown field')")
                .map(field -> "(doc['" + field + "'].size() == 0 ? '" + UNKNOWN_FIELD + "' : doc['" + field + "'].value)")
                .collect(Collectors.joining(" + '" + TERMS_SEPARATOR + "' + "));
    }

    private Boolean pivotHasFields(List<BucketSpec> pivots) {
        return pivots.stream()
                .map(BucketSpec::field)
                .findAny().isPresent();
    }

    private AggregationBuilder createAggregation(String name, List<BucketSpec> pivots, List<Order> bucketOrder) {
        final String aggregationScript = createScript(pivots);

        final TermsAggregationBuilder builder = AggregationBuilders.terms(name)
                .script(new Script(aggregationScript));

        return bucketOrder.isEmpty() ? builder : builder.order(bucketOrder);
    }


    private Stream<AggregationBuilder> seriesStream(Pivot pivot, ESGeneratedQueryContext queryContext, String reason) {
        return EntryStream.of(pivot.series())
                .mapKeyValue((integer, seriesSpec) -> {
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

    private boolean isAllMessagesTimeRange(TimeRange timeRange) {
        return ALL_MESSAGES_TIMERANGE.equals(timeRange);
    }

    private AbsoluteRange extractEffectiveTimeRange(SearchResult queryResult, Query query, Pivot pivot) {
        final Double from = queryResult.getAggregations().getMinAggregation("timestamp-min").getMin();
        final Double to = queryResult.getAggregations().getMaxAggregation("timestamp-max").getMax();
        final TimeRange pivotRange = query.effectiveTimeRange(pivot);
        return AbsoluteRange.create(
                isAllMessagesTimeRange(pivotRange) && from != null
                        ? new DateTime(from.longValue(), DateTimeZone.UTC)
                        : query.effectiveTimeRange(pivot).getFrom(),
                isAllMessagesTimeRange(pivotRange) && to != null
                        ? new DateTime(to.longValue(), DateTimeZone.UTC)
                        : query.effectiveTimeRange(pivot).getTo()
        );
    }

    private static List<String> copy(Collection<String> key) {
        return Collections.unmodifiableList(key.stream().collect(Collectors.toList()));
    }

    private static List<String> replaceUnknownFields(String key[]) {
        return copy(Arrays.stream(key).map(k -> UNKNOWN_FIELD.equals(k) ? null : k).collect(Collectors.toList()));
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Pivot pivot, SearchResult queryResult, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final AbsoluteRange effectiveTimerange = extractEffectiveTimeRange(queryResult, query, pivot);

        final PivotResult.Builder resultBuilder = PivotResult.builder()
                .id(pivot.id())
                .effectiveTimerange(effectiveTimerange)
                .total(extractDocumentCount(queryResult));

        // pivot results are a table where cells can contain multiple "values" and not only scalars:
        // each combination of row and column groups can contain all series (if rollup is true)
        // if rollup is false, only the "leaf" components contain the series
        // in the elasticsearch result, rows and columns are simply nested aggregations (first aggregations from rows, then from columns)
        // with metric aggregations on the corresponding levels.
        // first we iterate over all row groups (whose values generate a "key array", corresponding to the nesting level)
        // once we exhaust the row groups, we descend into the columns, which get added as values to their corresponding rows
        // on each nesting level and combination we have to check for series which we also add as values to the containing row

        // valid queryContext and queryResult are expected
        final String rowKey = pivot.id() + ROWS_POSTFIX;
        if(queryContext.contextMap().containsKey(rowKey)) {
            final String rowsAggName = queryContext.contextMap().get(rowKey).toString();
            final TermsAggregation rowsResult = queryResult.getAggregations().getTermsAggregation(rowsAggName);
            if (rowsResult != null) {
                final List<PivotResult.Row> rows = rowsResult.getBuckets()
                        .stream()
                        .map(bucket -> {
                            final List<String> rowKeys = replaceUnknownFields(bucket.getKeyAsString().split(TERMS_SEPARATOR));
                            if(pivot.rowGroups().size() != rowKeys.size()) {
                                LOG.warn("Expected number of terms in row: {}, but was {}. The separator char might be part of the field values.", pivot.rowGroups().size(), rowKeys.size());
                            }
                            final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(rowKeys).source("leaf");
                            if (pivot.rollup()) {
                                pivot.series().stream()
                                        .flatMap(seriesSpec -> createRowValuesForSeries(pivot, queryResult, queryContext, bucket, seriesSpec))
                                        .forEach(value -> rowBuilder.addValue(value));
                            }
                            if (!pivot.columnGroups().isEmpty()) {
                                final String columnsKey = pivot.id() + COLUMNS_POSTFIX;
                                if(queryContext.contextMap().containsKey(columnsKey)) {
                                    final String columnsAggName = queryContext.contextMap().get(columnsKey).toString();
                                    final TermsAggregation columnsResult = bucket.getTermsAggregation(columnsAggName);
                                    if (columnsResult != null) {
                                        columnsResult.getBuckets().forEach(columnBucket -> {
                                            final List<String> columnKeys = replaceUnknownFields(columnBucket.getKeyAsString().split(TERMS_SEPARATOR));
                                            if(pivot.columnGroups().size() != columnKeys.size()) {
                                                LOG.warn("Expected number of terms in columns: {}, but was {}. The separator char might be part of the field values.", pivot.rowGroups().size(), rowKeys.size());
                                            }
                                            pivot.series().stream()
                                                    .flatMap(seriesSpec -> createColumnValuesForSeries(pivot, queryResult, queryContext, columnBucket, seriesSpec, columnKeys))
                                                    .forEach(rowBuilder::addValue);
                                        });
                                    } else {
                                        LOG.warn("Expected aggregation '{}' not found in aggregations.", columnsAggName);
                                    }
                                } else {
                                    LOG.warn("Expected columns-key '{}' not found in queryContext", columnsKey);
                                }
                            }
                            return rowBuilder.build();
                        })
                        .collect(Collectors.toList());

                resultBuilder.addAllRows(rows);
            } else {
                LOG.warn("Expected aggregation '{}' not found in aggregations.", rowsAggName);
            }
        } else {
            // in the case that only columns were defined
            final String columnsKey = pivot.id() + COLUMNS_POSTFIX;
            if(queryContext.contextMap().containsKey(columnsKey)) {
                final String colsAggName = queryContext.contextMap().get(columnsKey).toString();
                final TermsAggregation columnsResult = queryResult.getAggregations().getTermsAggregation(colsAggName);
                if (columnsResult != null) {
                    final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.<String>builder().build()).source("leaf");

//                    if(pivot.rollup())
//                        processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), createInitialResult(queryResult), true, "row-leaf");

                    columnsResult.getBuckets().forEach(columnBucket -> {
                        final List<String> columnKeys = replaceUnknownFields(columnBucket.getKeyAsString().split(TERMS_SEPARATOR));
                        if(pivot.columnGroups().size() != columnKeys.size()) {
                            LOG.warn("Expected number of terms in columns: {}, but was {}. The separator char might be part of the field values.", pivot.columnGroups().size(), columnKeys.size());
                        }
                        pivot.series().stream()
                                .flatMap(seriesSpec -> createColumnValuesForSeries(pivot, queryResult, queryContext, columnBucket, seriesSpec, columnKeys))
                                .forEach(rowBuilder::addValue);
                    });

                    resultBuilder.addRow(rowBuilder.build());
                } else {
                    LOG.warn("Expected aggregation '{}' not found in aggregations.", colsAggName);
                }
            }
        }

        final PivotResult pvr = pivot.name().map(resultBuilder::name).orElse(resultBuilder).build();
        return pvr;
    }

    private Stream<PivotResult.Value> createRowValuesForSeries(Pivot pivot, SearchResult queryResult, ESGeneratedQueryContext queryContext, Bucket bucket, SeriesSpec seriesSpec) {
        return createValuesForSeries(pivot, queryResult, queryContext, bucket, seriesSpec, true, "row-leaf", Collections.emptyList());
    }

    private Stream<PivotResult.Value> createColumnValuesForSeries(Pivot pivot, SearchResult queryResult, ESGeneratedQueryContext queryContext, Bucket bucket, SeriesSpec seriesSpec, List<String> additionalKeys) {
        return createValuesForSeries(pivot, queryResult, queryContext, bucket, seriesSpec, false, "col-leaf", additionalKeys);
    }

    private Stream<PivotResult.Value> createValuesForSeries(Pivot pivot, SearchResult queryResult, ESGeneratedQueryContext queryContext, Bucket bucket, SeriesSpec seriesSpec, boolean rollup, String source, List<String> additionalKeys) {
        final ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> seriesHandler = seriesHandlers.get(seriesSpec.type());
        final Aggregation series = seriesHandler.extractAggregationFromResult(pivot, seriesSpec, bucket, queryContext);
        final Collection<String> keys = ImmutableList.<String>builder()
                .addAll(additionalKeys)
                .add(seriesSpec.literal())
                .build();
        return seriesHandler.handleResult(pivot, seriesSpec, queryResult, series, this, queryContext)
                .map(value -> PivotResult.Value.create(keys, value.value(), rollup, source));
    }

    private List<Order> orderListForPivot(Pivot pivot, ESGeneratedQueryContext esGeneratedQueryContext) {
        return pivot.sort()
                .stream()
                .map(sortSpec -> {
                    if (sortSpec instanceof PivotSort) {
                        return Order.term(sortSpec.direction().equals(SortSpec.Direction.Ascending));
                    }
                    if (sortSpec instanceof SeriesSort) {
                        final Optional<SeriesSpec> matchingSeriesSpec = pivot.series()
                                .stream()
                                .filter(series -> series.literal().equals(sortSpec.field()))
                                .findFirst();
                        return matchingSeriesSpec
                                .map(seriesSpec -> {
                                    if (seriesSpec.literal().equals("count()")) {
                                        return Order.count(sortSpec.direction().equals(SortSpec.Direction.Ascending));
                                    }
                                    return Order.aggregation(esGeneratedQueryContext.seriesName(seriesSpec, pivot), sortSpec.direction().equals(SortSpec.Direction.Ascending));
                                })
                                .orElse(null);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private long extractDocumentCount(SearchResult queryResult) {
        return queryResult.getTotal();
    }

    /*
        results from elasticsearch are nested so we need to recurse into the aggregation tree, but our result is a table, thus we need
        to keep track of the current row keys manually
         */
    private void processRows(PivotResult.Builder resultBuilder,
                             SearchResult searchResult,
                             ESGeneratedQueryContext queryContext,
                             Pivot pivot,
                             List<BucketSpec> remainingRows,
                             ArrayDeque<String> rowKeys,
                             MetricAggregation aggregation) {
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
            final ESPivotBucketSpecHandler<? extends PivotSpec, ? extends Aggregation> handler = bucketHandlers.get(currentBucket.type());
            final Aggregation aggregationResult = handler.extractAggregationFromResult(pivot, currentBucket, aggregation, queryContext);
            final Stream<ESPivotBucketSpecHandler.Bucket> bucketStream = handler.handleResult(pivot, currentBucket, searchResult, aggregationResult, this, queryContext);
            // for each bucket, recurse and eventually collect all the row keys. once we reach a leaf, we'll end up in the other if branch above
            bucketStream.forEach(bucket -> {
                // push the bucket's key and use its aggregation as the new source for sub-aggregations
                rowKeys.addLast(bucket.key());
                processRows(resultBuilder, searchResult, queryContext, pivot, tail(remainingRows), rowKeys, bucket.aggregation());
                rowKeys.removeLast();
            });
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
                                SearchResult searchResult,
                                ESGeneratedQueryContext queryContext,
                                Pivot pivot,
                                List<BucketSpec> remainingColumns,
                                ArrayDeque<String> columnKeys,
                                MetricAggregation aggregation) {
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
            final ESPivotBucketSpecHandler<? extends PivotSpec, ? extends Aggregation> handler = bucketHandlers.get(currentBucket.type());
            final Aggregation aggregationResult = handler.extractAggregationFromResult(pivot, currentBucket, aggregation, queryContext);
            final Stream<ESPivotBucketSpecHandler.Bucket> bucketStream = handler.handleResult(pivot, currentBucket, searchResult, aggregationResult, this, queryContext);

            // for each bucket, recurse and eventually collect all the column keys. once we reach a leaf, we'll end up in the other if branch above
            bucketStream.forEach(bucket -> {
                // push the bucket's key and use its aggregation as the new source for sub-aggregations
                columnKeys.addLast(bucket.key());
                processColumns(rowBuilder, searchResult, queryContext, pivot, tail(remainingColumns), columnKeys, bucket.aggregation());
                columnKeys.removeLast();
            });
            // also add the series for the base column key if the client wants rollups, the complete column key is processed in the leaf branch
            // don't add the empty column key rollup, because that's not the correct bucket here, it's being done in the row-leaf code
            if (pivot.rollup() && !columnKeys.isEmpty()) {
                // columnKeys is not empty, because this is a rollup per column in a row
                processSeries(rowBuilder, searchResult, queryContext, pivot, columnKeys, aggregation, true, "col-inner");
            }

        }
    }

    private void processSeries(PivotResult.Row.Builder rowBuilder,
                               SearchResult searchResult,
                               ESGeneratedQueryContext queryContext,
                               Pivot pivot,
                               ArrayDeque<String> columnKeys,
                               MetricAggregation aggregation,
                               boolean rollup,
                               String source) {
        pivot.series().forEach(seriesSpec -> {
            final ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> seriesHandler = seriesHandlers.get(seriesSpec.type());
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

        public Aggregation getSubAggregation(PivotSpec pivotSpec, MetricAggregation currentAggregationOrBucket) {
            final Tuple2<String, Class<? extends Aggregation>> tuple2 = getTypes(pivotSpec);
            return currentAggregationOrBucket.getAggregation(tuple2.v1, tuple2.v2);
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
