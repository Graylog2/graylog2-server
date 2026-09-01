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
package org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.aggregations.MissingBucketConstants;
import org.graylog.plugins.views.search.aggregations.OtherBucketDerivation;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.HasField;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.ValuesBucketOrdering;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.script.Script;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.BucketOrder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.Filters;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.ParsedFilters;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.Terms;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.ExtendedStats;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OtherBucket;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.PivotBucket;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog.plugins.views.search.aggregations.OtherBucketConstants.OTHER_BUCKET_NAME;

public class OSValuesHandler extends OSPivotBucketSpecHandler<Values> {
    private static final String KEY_SEPARATOR_CHARACTER = "\u2E31";
    private static final String KEY_SEPARATOR_PHRASE = " + \"" + KEY_SEPARATOR_CHARACTER + "\" + ";
    private static final String AGG_NAME = "agg";
    public static final BucketOrder DEFAULT_ORDER = BucketOrder.count(false);

    @Nonnull
    @Override
    public CreatedAggregations<AggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, Values bucketSpec, OSGeneratedQueryContext queryContext, Query query) {
        final var ordering = orderListForPivot(pivot, queryContext, DEFAULT_ORDER, query);
        final int limit = bucketSpec.limit();
        final List<String> orderedBuckets = ValuesBucketOrdering.orderFields(bucketSpec.fields(), pivot.sort());
        final var termsAggregation = createTerms(orderedBuckets, limit);

        termsAggregation.order(ordering.orders());
        ordering.sortingAggregations().forEach(termsAggregation::subAggregation);

        final FiltersAggregationBuilder filterAggregation = createFilter(name, orderedBuckets, bucketSpec.skipEmptyValues())
                .subAggregation(termsAggregation);

        final List<AggregationBuilder> metrics = List.of(termsAggregation, filterAggregation);
        if (bucketSpec.otherBucket()) {
            addStatsCompanions(pivot, metrics);
        }

        return CreatedAggregations.create(filterAggregation, termsAggregation, metrics);
    }

    /**
     * Mean-based series cannot be reconstructed for the {@code (Other)} bucket from their own value alone: the tail's
     * mean needs its value count and sum of squares. One {@code extended_stats} aggregation per field supplies all of
     * them, at both the terms level (the shown buckets) and the enclosing filter level (the total to subtract from).
     */
    private void addStatsCompanions(Pivot pivot, List<AggregationBuilder> metrics) {
        statsFields(pivot).forEach(field -> metrics.forEach(metric ->
                metric.subAggregation(AggregationBuilders.extendedStats(statsAggregationName(field)).field(field))));
    }

    /** The distinct fields whose {@code extended_stats} the derivation will need. */
    static List<String> statsFields(Pivot pivot) {
        return pivot.series().stream()
                .filter(OtherBucketDerivation::requiresStats)
                .map(HasField.class::cast)
                .map(HasField::field)
                .distinct()
                .sorted()
                .toList();
    }

    /** The {@code extended_stats} field for one series. Only call for specs where {@code requiresStats} is true. */
    static String statsFieldOf(SeriesSpec seriesSpec) {
        return ((HasField) seriesSpec).field();
    }

    public static String statsAggregationName(String field) {
        return "other-stats(" + field + ")";
    }

    private FiltersAggregationBuilder createFilter(String name, List<String> bucketSpecs, boolean skipEmptyValues) {
        final BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        bucketSpecs.stream()
                .map(QueryBuilders::existsQuery)
                .forEach(skipEmptyValues ? queryBuilder::must : queryBuilder::should);
        return AggregationBuilders.filters(name, queryBuilder)
                .otherBucket(true);
    }

    private TermsAggregationBuilder createTerms(List<String> valueBuckets, int limit) {
        return createScriptedTerms(valueBuckets, limit);
    }

    private TermsAggregationBuilder createScriptedTerms(List<String> buckets, int limit) {
        return AggregationBuilders.terms(AGG_NAME)
                .script(scriptForPivots(buckets))
                .size(limit);
    }

    private Script scriptForPivots(Collection<String> pivots) {
        final String scriptSource = Joiner.on(KEY_SEPARATOR_PHRASE).join(pivots.stream()
                .map(bucket -> """
                        (doc.containsKey('%1$s') && doc['%1$s'].size() > 0
                        ? doc['%1$s'].size() > 1
                            ? doc['%1$s']
                            : String.valueOf(doc['%1$s'].value)
                        : "%2$s")
                        """.formatted(bucket, MissingBucketConstants.MISSING_BUCKET_NAME))
                .collect(Collectors.toList()));
        return new Script(scriptSource);
    }

    @Override
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket initialBucket, OSGeneratedQueryContext queryContext) {
        var values = (Values) bucketSpecs;
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketsAggregation.Bucket previousBucket = initialBucket.bucket();
        final Function<List<String>, List<String>> reorderKeys = ValuesBucketOrdering.reorderFieldsFunction(bucketSpecs.fields(), pivot.sort());

        final Aggregation aggregation = previousBucket.getAggregations().get(AGG_NAME);
        if (!(aggregation instanceof final ParsedFilters filterAggregation)) {
            // This happens when the other bucket is passed for column value extraction
            return Stream.of(initialBucket);
        }
        final Filters.Bucket presentValuesBucket = filterAggregation.getBuckets().get(0);
        final MultiBucketsAggregation termsAggregation = presentValuesBucket.getAggregations().get(AGG_NAME);

        Stream<PivotBucket> bucketStream = extractTermsBuckets(previousKeys, reorderKeys, termsAggregation);

        if (!values.skipEmptyValues()) {
            final Filters.Bucket emptyValuesBucket = filterAggregation.getBuckets().get(1);
            if (emptyValuesBucket.getDocCount() > 0) {
                // filters[1] holds only documents where none of the grouped fields exist, so its scripted key is
                // always the single all-(Empty Value) tuple: no tail to contribute, and its documents are absent
                // from filters[0], so its buckets must never be subtracted from filters[0]'s totals.
                final MultiBucketsAggregation emptyValuesTerms = emptyValuesBucket.getAggregations().get(AGG_NAME);
                bucketStream = Stream.concat(bucketStream, extractTermsBuckets(previousKeys, reorderKeys, emptyValuesTerms));
            }
        }

        if (values.otherBucket()) {
            final Optional<PivotBucket> otherBucket =
                    createOtherBucket(pivot, values, previousKeys, presentValuesBucket, termsAggregation, queryContext);
            if (otherBucket.isPresent()) {
                bucketStream = Stream.concat(bucketStream, Stream.of(otherBucket.get()));
            }
        }

        return bucketStream;
    }

    private Optional<PivotBucket> createOtherBucket(Pivot pivot,
                                                     Values values,
                                                     ImmutableList<String> previousKeys,
                                                     HasAggregations parent,
                                                     MultiBucketsAggregation termsAggregation,
                                                     OSGeneratedQueryContext queryContext) {
        if (!(termsAggregation instanceof final Terms terms)) {
            return Optional.empty();
        }
        final long otherDocCount = terms.getSumOfOtherDocCounts();

        if (otherDocCount <= 0) {
            return Optional.empty();
        }

        final List<MultiBucketsAggregation.Bucket> shownBuckets = termsAggregation.getBuckets().stream()
                .map(MultiBucketsAggregation.Bucket.class::cast)
                .toList();

        final Map<String, Object> derivedValues = new LinkedHashMap<>();
        pivot.series().stream()
                .filter(OtherBucketDerivation::isDerivable)
                .forEach(seriesSpec ->
                        tailInput(pivot, seriesSpec, otherDocCount, parent, shownBuckets, queryContext)
                                .flatMap(input -> OtherBucketDerivation.derive(seriesSpec, input))
                                .ifPresent(value -> derivedValues.put(seriesSpec.id(), value)));

        if (derivedValues.isEmpty()) {
            return Optional.empty();
        }

        final ImmutableList<String> keys = ImmutableList.<String>builder()
                .addAll(previousKeys)
                .addAll(Collections.nCopies(values.fields().size(), OTHER_BUCKET_NAME))
                .build();

        return Optional.of(PivotBucket.createOther(keys, OtherBucket.create(otherDocCount), derivedValues));
    }

    /**
     * Fails closed: if any shown bucket is missing the value or stats needed for this series, the whole series is
     * omitted from the {@code (Other)} bucket rather than derived from a partial, too-large tail.
     */
    private Optional<OtherBucketDerivation.TailInput> tailInput(Pivot pivot,
                                                                 SeriesSpec seriesSpec,
                                                                 long otherDocCount,
                                                                 HasAggregations parent,
                                                                 List<MultiBucketsAggregation.Bucket> shownBuckets,
                                                                 OSGeneratedQueryContext queryContext) {
        if (OtherBucketDerivation.requiresStats(seriesSpec)) {
            final String statsName = statsAggregationName(statsFieldOf(seriesSpec));
            final List<OtherBucketDerivation.Stats> shownStats = shownBuckets.stream()
                    .map(bucket -> readStats(bucket, statsName))
                    .toList();
            if (shownStats.stream().anyMatch(Objects::isNull)) {
                return Optional.empty();
            }
            return Optional.of(new OtherBucketDerivation.TailInput(
                    otherDocCount,
                    null,
                    List.of(),
                    readStats(parent, statsName),
                    shownStats));
        }

        if (!OtherBucketDerivation.requiresSeriesValue(seriesSpec)) {
            return Optional.of(new OtherBucketDerivation.TailInput(otherDocCount, null, List.of(), null, List.of()));
        }

        final String seriesName = queryContext.seriesName(seriesSpec, pivot);
        final List<Double> shownValues = shownBuckets.stream()
                .map(bucket -> readNumeric(bucket, seriesName))
                .toList();
        if (shownValues.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        return Optional.of(new OtherBucketDerivation.TailInput(
                otherDocCount,
                readNumeric(parent, seriesName),
                shownValues,
                null,
                List.of()));
    }

    private static OtherBucketDerivation.Stats readStats(HasAggregations source, String name) {
        final Aggregation aggregation = source.getAggregations().get(name);
        if (!(aggregation instanceof final ExtendedStats stats)) {
            return null;
        }
        return new OtherBucketDerivation.Stats(stats.getCount(), stats.getSum(), stats.getSumOfSquares());
    }

    private static Double readNumeric(HasAggregations source, String name) {
        final Aggregation aggregation = source.getAggregations().get(name);
        if (aggregation instanceof final NumericMetricsAggregation.SingleValue singleValue) {
            return singleValue.value();
        }
        return null;
    }

    private Stream<PivotBucket> extractTermsBuckets(ImmutableList<String> previousKeys, Function<List<String>, List<String>> reorderKeys, MultiBucketsAggregation termsAggregation) {
        return termsAggregation.getBuckets().stream()
                .map(bucket -> {
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .addAll(reorderKeys.apply(extractKeys(bucket)))
                            .build();

                    return PivotBucket.create(keys, bucket);
                });
    }

    private List<String> extractKeys(MultiBucketsAggregation.Bucket bucket) {
        return splitKeys(bucket.getKeyAsString());
    }

    private ImmutableList<String> splitKeys(String keys) {
        return ImmutableList.copyOf(Splitter.on(KEY_SEPARATOR_CHARACTER).split(keys));
    }
}
