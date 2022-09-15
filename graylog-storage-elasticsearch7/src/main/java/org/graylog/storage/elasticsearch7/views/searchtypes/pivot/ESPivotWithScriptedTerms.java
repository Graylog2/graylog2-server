package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import one.util.streamex.EntryStream;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
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
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.MinAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ESPivotWithScriptedTerms implements ESSearchTypeHandler<Pivot> {
    private static final Logger LOG = LoggerFactory.getLogger(ESPivot.class);
    private static final String KEY_SEPARATOR_CHARACTER = "\u2E31";
    private static final String KEY_SEPARATOR_PHRASE = " + \"" + KEY_SEPARATOR_CHARACTER + "\" + ";
    private static final String AGG_NAME = "agg";

    private final Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;
    private final EffectiveTimeRangeExtractor effectiveTimeRangeExtractor;

    @Inject
    public ESPivotWithScriptedTerms(Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers,
                                    EffectiveTimeRangeExtractor effectiveTimeRangeExtractor) {
        this.seriesHandlers = seriesHandlers;
        this.effectiveTimeRangeExtractor = effectiveTimeRangeExtractor;
    }

    @Override
    public void doGenerateQueryPart(Query query, Pivot pivot, ESGeneratedQueryContext queryContext) {
        LOG.debug("Generating aggregation for {}", pivot);
        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(pivot);

        final Map<Object, Object> contextMap = queryContext.contextMap();
        final ESPivot.AggTypes aggTypes = new ESPivot.AggTypes();
        contextMap.put(pivot.id(), aggTypes);

        // add global rollup series if those were requested
        /*if (pivot.rollup()) {
            seriesStream(pivot, queryContext, "global rollup")
                    .forEach(searchSourceBuilder::aggregation);
        }*/

        final String scriptSource = Joiner.on(KEY_SEPARATOR_PHRASE).join(pivot.rowGroups().stream()
                .map(bucket -> "doc['" + bucket.field() + "'].value")
                .collect(Collectors.toList()));
        final List<BucketOrder> ordering = orderListForPivot(pivot, queryContext);
        final TermsAggregationBuilder termsAggregation = AggregationBuilders.terms(AGG_NAME)
                .script(new Script(scriptSource))
                .size(15)
                .order(ordering.isEmpty() ? List.of(BucketOrder.count(false)) : ordering);
        seriesStream(pivot, queryContext, "metrics")
                .forEach(termsAggregation::subAggregation);
        searchSourceBuilder.aggregation(termsAggregation);

        final MinAggregationBuilder startTimestamp = AggregationBuilders.min("timestamp-min").field("timestamp");
        final MaxAggregationBuilder endTimestamp = AggregationBuilders.max("timestamp-max").field("timestamp");
        searchSourceBuilder.aggregation(startTimestamp);
        searchSourceBuilder.aggregation(endTimestamp);
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

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Pivot pivot, SearchResponse queryResult, Aggregations aggregations, ESGeneratedQueryContext queryContext) {
        final AbsoluteRange effectiveTimerange = this.effectiveTimeRangeExtractor.extract(queryResult, query, pivot);

        final PivotResult.Builder resultBuilder = PivotResult.builder()
                .id(pivot.id())
                .effectiveTimerange(effectiveTimerange)
                .total(extractDocumentCount(queryResult));

        final Terms termsResults = aggregations.get(AGG_NAME);
        termsResults.getBuckets()
                .forEach(bucket -> {
                    final ImmutableList<String> keys = ImmutableList.copyOf(Splitter.on(KEY_SEPARATOR_CHARACTER).split(bucket.getKeyAsString()));
                    final PivotResult.Row.Builder rowBuilder = PivotResult.Row.builder()
                            .key(keys)
                            .source("leaf");
                    processSeries(rowBuilder, queryResult, queryContext, pivot, new ArrayDeque<>(keys), bucket, false, "row-leaf");
                    resultBuilder.addRow(rowBuilder.build());
                });

        return resultBuilder.build();

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
                        final PivotResult.Value v = PivotResult.Value.create(List.of(value.id()), value.value(), rollup, source);
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
