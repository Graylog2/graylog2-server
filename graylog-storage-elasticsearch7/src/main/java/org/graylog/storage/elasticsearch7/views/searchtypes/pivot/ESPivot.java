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
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.script.Script;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.BucketOrder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.HasAggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Max;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Min;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.MinAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
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

    private static final String ROWS_POSTFIX = "-rows";
    private static final String COLUMNS_POSTFIX = "-columns";

    private static TimeRange allMessagesTimeRange() {
        try {
            return RelativeRange.create(0);
        } catch (InvalidRangeParametersException e) {
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

        final List<BucketOrder> bucketOrder = orderListForPivot(pivot, queryContext);

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

    private AggregationBuilder createAggregation(String name, List<BucketSpec> pivots, List<BucketOrder> bucketOrder) {
        final String aggregationScript = createScript(pivots);

        final TermsAggregationBuilder builder = AggregationBuilders.terms(name)
                .script(new Script(aggregationScript));

        return bucketOrder.isEmpty() ? builder : builder.order(bucketOrder);
    }

    private List<BucketOrder> orderListForPivot(Pivot pivot, ESGeneratedQueryContext esGeneratedQueryContext) {
        return pivot.sort()
                .stream()
                .map(sortSpec -> {
                    if (sortSpec instanceof PivotSort) {
                        return BucketOrder.key(sortSpec.direction().equals(SortSpec.Direction.Ascending));
                    }
                    if (sortSpec instanceof SeriesSort) {
                        final Optional<SeriesSpec> matchingSeriesSpec = pivot.series()
                                .stream()
                                .filter(series -> series.literal().equals(sortSpec.field()))
                                .findFirst();
                        return matchingSeriesSpec
                                .map(seriesSpec -> {
                                    if (seriesSpec.literal().equals("count()")) {
                                        return BucketOrder.count(sortSpec.direction().equals(SortSpec.Direction.Ascending));
                                    }
                                    return BucketOrder.aggregation(esGeneratedQueryContext.seriesName(seriesSpec, pivot), sortSpec.direction().equals(SortSpec.Direction.Ascending));
                                })
                                .orElse(null);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String createScript(List<BucketSpec> pivots) {
        return pivots.stream()
                .map(BucketSpec::field)
                .map(field -> field == null ? "'null field'" : "(doc.containsKey('" + field + "') ? doc['" + field + "'].value : 'unknown field')")
                .collect(Collectors.joining(" + '" + TERMS_SEPARATOR + "' + "));
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

    private AbsoluteRange extractEffectiveTimeRange(SearchResponse queryResult, Query query, Pivot pivot) {
        final Min min = queryResult.getAggregations().get("timestamp-min");
        final Double from = min.getValue();
        final Max max = queryResult.getAggregations().get("timestamp-max");
        final Double to = max.getValue();
        final TimeRange pivotRange = query.effectiveTimeRange(pivot);
        return AbsoluteRange.create(
                isAllMessagesTimeRange(pivotRange) && from != 0
                        ? new DateTime(from.longValue(), DateTimeZone.UTC)
                        : query.effectiveTimeRange(pivot).getFrom(),
                isAllMessagesTimeRange(pivotRange) && to != 0
                        ? new DateTime(to.longValue(), DateTimeZone.UTC)
                        : query.effectiveTimeRange(pivot).getTo()
        );
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Pivot pivot, SearchResponse queryResult, Aggregations aggregations, ESGeneratedQueryContext queryContext) {
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
            final MultiBucketsAggregation rowsResult = queryResult.getAggregations().get(rowsAggName);
            if (rowsResult != null) {
                final List<PivotResult.Row> rows = rowsResult.getBuckets()
                        .stream()
                        .map(bucket -> {
                            final ImmutableList<String> rowKeys = ImmutableList.copyOf(bucket.getKeyAsString().split(TERMS_SEPARATOR));
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
                                    final MultiBucketsAggregation columnsResult = bucket.getAggregations().get(columnsAggName);
                                    if (columnsResult != null) {
                                        columnsResult.getBuckets().forEach(columnBucket -> {
                                            final ImmutableList<String> columnKeys = ImmutableList.copyOf(columnBucket.getKeyAsString().split(TERMS_SEPARATOR));
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
                final MultiBucketsAggregation columnsResult = queryResult.getAggregations().get(colsAggName);
                if (columnsResult != null) {
                    final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.<String>builder().build()).source("leaf");

                    if(pivot.rollup())
                        processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), createInitialResult(queryResult), true, "row-leaf");

                    columnsResult.getBuckets().forEach(columnBucket -> {
                        final ImmutableList<String> columnKeys = ImmutableList.copyOf(columnBucket.getKeyAsString().split(TERMS_SEPARATOR));
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

    private Stream<PivotResult.Value> createRowValuesForSeries(Pivot pivot, SearchResponse queryResult, ESGeneratedQueryContext queryContext, MultiBucketsAggregation.Bucket bucket, SeriesSpec seriesSpec) {
        return createValuesForSeries(pivot, queryResult, queryContext, bucket, seriesSpec, true, "row-leaf", Collections.emptyList());
    }

    private Stream<PivotResult.Value> createColumnValuesForSeries(Pivot pivot, SearchResponse queryResult, ESGeneratedQueryContext queryContext, MultiBucketsAggregation.Bucket bucket, SeriesSpec seriesSpec, List<String> additionalKeys) {
        return createValuesForSeries(pivot, queryResult, queryContext, bucket, seriesSpec, false, "col-leaf", additionalKeys);
    }

    private Stream<PivotResult.Value> createValuesForSeries(Pivot pivot, SearchResponse queryResult, ESGeneratedQueryContext queryContext, MultiBucketsAggregation.Bucket bucket, SeriesSpec seriesSpec, boolean rollup, String source, List<String> additionalKeys) {
        final ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> seriesHandler = seriesHandlers.get(seriesSpec.type());
        final Aggregation series = seriesHandler.extractAggregationFromResult(pivot, seriesSpec, bucket, queryContext);
        final Collection<String> keys = ImmutableList.<String>builder()
                .addAll(additionalKeys)
                .add(seriesSpec.literal())
                .build();
        return seriesHandler.handleResult(pivot, seriesSpec, queryResult, series, this, queryContext)
                .map(value -> PivotResult.Value.create(keys, value.value(), rollup, source));
    }

    private HasAggregations createInitialResult(SearchResponse queryResult) {
        return InitialBucket.create(queryResult);
    }

    private long extractDocumentCount(final SearchResponse queryResult) {
        return queryResult.getHits().getTotalHits().value;
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
