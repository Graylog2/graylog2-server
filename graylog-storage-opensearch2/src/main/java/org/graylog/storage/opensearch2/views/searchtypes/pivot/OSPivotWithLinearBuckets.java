package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import one.util.streamex.EntryStream;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.script.Script;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.BucketOrder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.Terms;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.support.MultiTermsValuesSourceConfig;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets.OSTimeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.storage.DetectedSearchVersion;
import org.graylog2.storage.SearchVersion;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OSPivotWithLinearBuckets implements OSSearchTypeHandler<Pivot> {
    private static final Logger LOG = LoggerFactory.getLogger(OSPivotWithLinearBuckets.class);
    private static final String KEY_SEPARATOR_CHARACTER = "\u2E31";
    private static final String KEY_SEPARATOR_PHRASE = " + \"" + KEY_SEPARATOR_CHARACTER + "\" + ";
    private static final String AGG_NAME = "agg";

    private final Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;
    private final EffectiveTimeRangeExtractor effectiveTimeRangeExtractor;
    private final OSTimeHandler osTimeHandler;
    private final boolean supportsMultiTerms;

    @Inject
    public OSPivotWithLinearBuckets(Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers,
                                    EffectiveTimeRangeExtractor effectiveTimeRangeExtractor,
                                    OSTimeHandler osTimeHandler,
                                    @DetectedSearchVersion SearchVersion version) {
        this.seriesHandlers = seriesHandlers;
        this.effectiveTimeRangeExtractor = effectiveTimeRangeExtractor;
        this.osTimeHandler = osTimeHandler;
        this.supportsMultiTerms = version.satisfies(SearchVersion.Distribution.OPENSEARCH, ">=2.2.0");
    }

    @Override
    public void doGenerateQueryPart(Query query, Pivot pivot, OSGeneratedQueryContext queryContext) {
        LOG.debug("Generating aggregation for {}", pivot);
        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(pivot);

        final Map<Object, Object> contextMap = queryContext.contextMap();
        final OSPivot.AggTypes aggTypes = new OSPivot.AggTypes();
        contextMap.put(pivot.id(), aggTypes);

        // add global rollup series if those were requested
        if (pivot.rollup()) {
            seriesStream(pivot, queryContext, "global rollup")
                    .forEach(searchSourceBuilder::aggregation);
        }

        final List<BucketOrder> ordering = orderListForPivot(pivot, queryContext);
        final Tuple2<AggregationBuilder, AggregationBuilder> aggregationTuple = createPivots(query, pivot, pivot.rowGroups(), queryContext, ordering);
        final AggregationBuilder rootAggregation = aggregationTuple.v1();
        final AggregationBuilder leafAggregation = aggregationTuple.v2();
        if (pivot.columnGroups().isEmpty() || pivot.rollup()) {
            seriesStream(pivot, queryContext, "metrics")
                    .forEach(aggregation -> {
                        if (leafAggregation != null) {
                            leafAggregation.subAggregation(aggregation);
                        } else {
                            searchSourceBuilder.aggregation(aggregation);
                        }
                    });
        }

        if (!pivot.columnGroups().isEmpty()) {
            final Tuple2<AggregationBuilder, AggregationBuilder> columnsAggregationTuple = createPivots(query, pivot, pivot.columnGroups(), queryContext, ordering);
            final AggregationBuilder columnsRootAggregation = columnsAggregationTuple.v1();
            final AggregationBuilder columnsLeafAggregation = columnsAggregationTuple.v2();
            seriesStream(pivot, queryContext, "metrics")
                    .forEach(columnsLeafAggregation::subAggregation);
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

    private Tuple2<AggregationBuilder, AggregationBuilder> createPivots(Query query, Pivot pivot, List<BucketSpec> pivots, OSGeneratedQueryContext queryContext, List<BucketOrder> ordering) {
        AggregationBuilder aggregationBuilder = null;
        AggregationBuilder root = null;
        List<Values> valueBuckets = new ArrayList<>();
        for (BucketSpec bucketSpec : pivots) {
            if (bucketSpec instanceof Time time) {
                if (!valueBuckets.isEmpty()) {
                    final AggregationBuilder termsAggregation = createTerms(valueBuckets, ordering) ;
                    if (aggregationBuilder == null) {
                        aggregationBuilder = termsAggregation;
                        root = termsAggregation;
                    } else {
                        aggregationBuilder.subAggregation(termsAggregation);
                    }
                    valueBuckets = new ArrayList<>();
                }
                final AggregationBuilder datePivot = osTimeHandler.doCreateAggregation(AGG_NAME, pivot, time, queryContext, query)
                        .orElseThrow(() -> new IllegalStateException("Time handler unexpectedly returns no value."));
                if (aggregationBuilder == null) {
                    aggregationBuilder = datePivot;
                    root = datePivot;
                } else {
                    aggregationBuilder.subAggregation(datePivot);
                }
            }
            if (bucketSpec instanceof Values values) {
                valueBuckets.add(values);
            }
        }
        if (!valueBuckets.isEmpty()) {
            final AggregationBuilder termsAggregation = createTerms(valueBuckets, ordering);
            if (aggregationBuilder == null) {
                aggregationBuilder = termsAggregation;
                root = termsAggregation;
            } else {
                aggregationBuilder.subAggregation(termsAggregation);
            }
        }

        return new Tuple2<>(root, aggregationBuilder);
    }

    private AggregationBuilder createTerms(List<Values> valueBuckets, List<BucketOrder> ordering) {
        return valueBuckets.size() > 1
                ? supportsMultiTerms
                    ? createMultiTerms(valueBuckets, ordering)
                    : createScriptedTerms(valueBuckets, ordering)
                : createSimpleTerms(valueBuckets.get(0), ordering);
    }

    private AggregationBuilder createSimpleTerms(Values values, List<BucketOrder> ordering) {
        return AggregationBuilders.terms(AGG_NAME)
                .field(values.field())
                .order(ordering)
                .size(15);
    }

    private AggregationBuilder createMultiTerms(List<Values> valueBuckets, List<BucketOrder> ordering) {
        return AggregationBuilders.multiTerms(AGG_NAME)
                .terms(valueBuckets.stream()
                        .map(value -> new MultiTermsValuesSourceConfig.Builder()
                        .setFieldName(value.field())
                        .build())
                        .collect(Collectors.toList()))
                .order(ordering)
                .size(15);
    }

    private TermsAggregationBuilder createScriptedTerms(List<? extends BucketSpec> buckets, List<BucketOrder> ordering) {
        return AggregationBuilders.terms(AGG_NAME)
                .script(scriptForPivots(buckets))
                .size(15)
                .order(ordering.isEmpty() ? List.of(BucketOrder.count(false)) : ordering);
    }

    private Script scriptForPivots(Collection<? extends BucketSpec> pivots) {
        final String scriptSource = Joiner.on(KEY_SEPARATOR_PHRASE).join(pivots.stream()
                .map(bucket -> "doc['" + bucket.field() + "'].value")
                .collect(Collectors.toList()));
        return new Script(scriptSource);
    }

    private List<BucketOrder> orderListForPivot(Pivot pivot, OSGeneratedQueryContext esGeneratedQueryContext) {
        final List<BucketOrder> ordering = pivot.sort()
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
        return ordering.isEmpty() ? List.of(BucketOrder.count(false)) : ordering;
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

        retrieveBuckets(pivot.rowGroups(), queryResult)
                .forEach(tuple -> {
                    final ImmutableList<String> keys = supportsMultiTerms ? extractMultiTermsKeys(tuple.v2()) : tuple.v1();
                    final MultiBucketsAggregation.Bucket bucket = tuple.v2();
                    final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder()
                            .key(keys)
                            .source("leaf");
                    if (pivot.columnGroups().isEmpty() || pivot.rollup()) {
                        processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), bucket, true, "row-leaf");
                    }
                    if (!pivot.columnGroups().isEmpty()){
                        final Terms columnsResults = bucket.getAggregations().get(AGG_NAME);
                        columnsResults.getBuckets()
                                .forEach(columnBucket -> {
                                    final ImmutableList<String> columnKeys = supportsMultiTerms ? extractMultiTermsKeys(columnBucket) : splitKeys(columnBucket.getKeyAsString());

                                    processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(columnKeys), columnBucket, false, "col-leaf");
                                });
                    }
                    resultBuilder.addRow(rowBuilder.build());
                });

        if (!pivot.rowGroups().isEmpty() && pivot.rollup()) {
            final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder().key(ImmutableList.of());
            processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(), createInitialResult(queryResult), true, "row-leaf");
            resultBuilder.addRow(rowBuilder.source("leaf").build());
        }

        return resultBuilder.build();
    }

    private HasAggregations createInitialResult(SearchResponse queryResult) {
        return InitialBucket.create(queryResult);
    }

    private ImmutableList<String> extractMultiTermsKeys(MultiBucketsAggregation.Bucket bucket) {
        final Object key = bucket.getKey();
        if (key == null) {
            return ImmutableList.of();
        }
        if (key instanceof Collection) {
            //noinspection unchecked
            return ((Collection<Object>) key).stream()
                        .map(String::valueOf)
                        .collect(ImmutableList.toImmutableList());
        }
        if (key instanceof String) {
            return ImmutableList.of((String)key);
        }

        return ImmutableList.of(String.valueOf(key));
    }
    private int measureDepth(List<? extends BucketSpec> pivots) {
        int depth = 0;
        List<Values> valueBuckets = new ArrayList<>();
        for (BucketSpec bucketSpec : pivots) {
            if (bucketSpec instanceof Time) {
                if (!valueBuckets.isEmpty()) {
                    depth += 1;
                    valueBuckets = new ArrayList<>();
                }
                depth += 1;
            }
            if (bucketSpec instanceof Values values) {
                valueBuckets.add(values);
            }
        }

        if (!valueBuckets.isEmpty()) {
            depth += 1;
        }

        return depth;
    }

    private Stream<Tuple2<ImmutableList<String>, ? extends MultiBucketsAggregation.Bucket>> retrieveBuckets(List<? extends BucketSpec> pivots, SearchResponse queryResult) {
        final Aggregations aggregations = queryResult.getAggregations();
        final int depth = measureDepth(pivots);

        if (depth == 0) {
            final MultiBucketsAggregation.Bucket singleBucket = createSingleBucket(queryResult);
            return Stream.of(new Tuple2<>(ImmutableList.of(), singleBucket));
        }

        final MultiBucketsAggregation agg = aggregations.get(AGG_NAME);
        Stream<Tuple2<ImmutableList<String>, ? extends MultiBucketsAggregation.Bucket>> result = agg.getBuckets()
                .stream()
                .map(bucket -> new Tuple2<>(splitKeys(bucket.getKeyAsString()), bucket));

        for (int i = 1; i < depth; i++) {
            result = result.flatMap((tuple) -> {
                final ImmutableList<String> previousKeys = tuple.v1();
                final MultiBucketsAggregation.Bucket previousBucket = tuple.v2();

                final MultiBucketsAggregation aggregation = previousBucket.getAggregations().get(AGG_NAME);
                return aggregation.getBuckets().stream()
                        .map(bucket -> {
                            final ImmutableList<String> keys = ImmutableList.<String>builder()
                                    .addAll(previousKeys)
                                    .addAll(splitKeys(bucket.getKeyAsString()))
                                    .build();

                            return new Tuple2<>(keys, bucket);
                        });
            });
        }

        return result;
    }

    private MultiBucketsAggregation.Bucket createSingleBucket(SearchResponse queryResult) {
        return new MultiBucketsAggregation.Bucket() {
            @Override
            public Object getKey() {
                return null;
            }

            @Override
            public String getKeyAsString() {
                return null;
            }

            @Override
            public long getDocCount() {
                return extractDocumentCount(queryResult);
            }

            @Override
            public Aggregations getAggregations() {
                return queryResult.getAggregations();
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                return null;
            }
        };
    }

    private ImmutableList<String> splitKeys(String keys) {
        return ImmutableList.copyOf(Splitter.on(KEY_SEPARATOR_CHARACTER).split(keys));
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
