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
package org.graylog.storage.opensearch3.views.searchtypes.pivot.buckets;

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
import org.graylog.storage.opensearch3.OSSerializationUtils;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OtherBucket;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.PivotBucket;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.AggregateVariant;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersAggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersBucket;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.aggregations.SingleMetricAggregateBase;
import org.opensearch.client.opensearch._types.aggregations.TermsAggregateBase;
import org.opensearch.client.opensearch._types.aggregations.TermsAggregation;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.ExistsQuery;

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
    public static final BucketOrder DEFAULT_ORDER = BucketOrder.count(SortOrder.Desc);

    @Nonnull
    @Override
    public CreatedAggregations<MutableNamedAggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, Values bucketSpec, OSGeneratedQueryContext queryContext, Query query) {
        final var ordering = orderListForPivot(pivot, queryContext, DEFAULT_ORDER, query);
        final int limit = bucketSpec.limit();
        final List<String> orderedBuckets = ValuesBucketOrdering.orderFields(bucketSpec.fields(), pivot.sort());
        final TermsAggregation.Builder termsAggregationBuilder = createTerms(orderedBuckets, limit);

        termsAggregationBuilder.order(mapOrders(ordering.orders()));
        final MutableNamedAggregationBuilder termsAggregation = new MutableNamedAggregationBuilder(
                AGG_NAME,
                Aggregation.builder()
                        .terms(termsAggregationBuilder.build())
                        .aggregations(ordering.sortingAggregations())
        );

        final FiltersAggregation.Builder filterAggregationBuilder = createFilter(orderedBuckets, bucketSpec.skipEmptyValues());
        final MutableNamedAggregationBuilder filterAggregation = new MutableNamedAggregationBuilder(
                name,
                Aggregation.builder()
                        .filters(filterAggregationBuilder.build())
        );
        filterAggregation.subAggregation(termsAggregation);

        final List<MutableNamedAggregationBuilder> metrics = List.of(termsAggregation, filterAggregation);
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
    private void addStatsCompanions(Pivot pivot, List<MutableNamedAggregationBuilder> metrics) {
        statsFields(pivot).forEach(field -> metrics.forEach(metric ->
                metric.subAggregation(new MutableNamedAggregationBuilder(
                        statsAggregationName(field),
                        Aggregation.builder().extendedStats(e -> e.field(field))))));
    }

    /**
     * The distinct fields whose {@code extended_stats} the derivation will need. Public because
     * {@code PivotQueryGenerator}, in the parent package, also attaches these companions to the second column tree
     * it builds for the {@code (Other)} row's per-column cells.
     */
    public static List<String> statsFields(Pivot pivot) {
        return pivot.series().stream()
                .filter(OtherBucketDerivation::requiresStats)
                .map(HasField.class::cast)
                .map(HasField::field)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * The {@code extended_stats} field for one series. Only call for specs where {@code requiresStats} is true.
     * Public for the same reason as {@link #statsFields}: {@code PivotResultProcessor} needs it to read the
     * companion back when deriving the {@code (Other)} row's per-column cells.
     */
    public static String statsFieldOf(SeriesSpec seriesSpec) {
        return ((HasField) seriesSpec).field();
    }

    public static String statsAggregationName(String field) {
        return "other-stats(" + field + ")";
    }

    private FiltersAggregation.Builder createFilter(List<String> bucketSpecs, boolean skipEmptyValues) {
        BoolQuery.Builder boolQuery = BoolQuery.builder();
        bucketSpecs.stream()
                .map(spec -> ExistsQuery.builder().field(spec).build().toQuery())
                .forEach(skipEmptyValues ? boolQuery::must : boolQuery::should);
        return FiltersAggregation.builder()
                .filters(b -> b.array(List.of(boolQuery.build().toQuery())))
                .otherBucket(true);
    }

    private List<Map<String, SortOrder>> mapOrders(List<BucketOrder> orders) {
        return orders.stream()
                .map(order -> {
                    if (order.type() == BucketOrder.Type.KEY) {
                        return Map.of("_key", order.order());
                    } else if (order.type() == BucketOrder.Type.COUNT) {
                        return Map.of("_count", order.order());
                    } else if (order.type() == BucketOrder.Type.AGGREGATION) {
                        return Map.of(order.name(), order.order());
                    } else {
                        throw new IllegalArgumentException("Unknown order type: " + order.type());
                    }
                })
                .collect(Collectors.toList());
    }

    private TermsAggregation.Builder createTerms(List<String> valueBuckets, int limit) {
        return createScriptedTerms(valueBuckets, limit);
    }

    private TermsAggregation.Builder createScriptedTerms(List<String> buckets, int limit) {
        return TermsAggregation.builder()
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
        return Script.of(b -> b.inline(s -> s.source(scriptSource)));
    }

    @Override
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket initialBucket, OSGeneratedQueryContext queryContext) {
        var values = (Values) bucketSpecs;
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketBase previousBucket = initialBucket.bucket();
        final Function<List<String>, List<String>> reorderKeys = ValuesBucketOrdering.reorderFieldsFunction(bucketSpecs.fields(), pivot.sort());

        final Aggregate aggregation = previousBucket.aggregations().get(AGG_NAME);
        if (!aggregation.isFilters()) {
            // This happens when the other bucket is passed for column value extraction
            return Stream.of(initialBucket);
        }
        final FiltersBucket presentValuesBucket = aggregation.filters().buckets().array().getFirst();
        final Aggregate termsAggregation = presentValuesBucket.aggregations().get(AGG_NAME);

        Stream<PivotBucket> bucketStream = extractTermsBuckets(previousKeys, reorderKeys, termsAggregation);

        if (!values.skipEmptyValues()) {
            final FiltersBucket emptyValuesBucket = aggregation.filters().buckets().array().get(1);
            if (emptyValuesBucket.docCount() > 0) {
                // filters[1] holds only documents where none of the grouped fields exist, so its scripted key is
                // always the single all-(Empty Value) tuple: no tail to contribute, and its documents are absent
                // from filters[0], so its buckets must never be subtracted from filters[0]'s totals.
                final Aggregate emptyValuesTerms = emptyValuesBucket.aggregations().get(AGG_NAME);
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
                                                     MultiBucketBase parent,
                                                     Aggregate termsAggregation,
                                                     OSGeneratedQueryContext queryContext) {
        final AggregateVariant rawAggregation = termsAggregation._get();
        if (!(rawAggregation instanceof final TermsAggregateBase<?> terms)) {
            return Optional.empty();
        }
        final long otherDocCount = Optional.ofNullable(terms.sumOtherDocCount()).orElse(0L);

        if (otherDocCount <= 0) {
            return Optional.empty();
        }

        final List<MultiBucketBase> shownBuckets = terms.buckets().array().stream()
                .map(bucket -> {
                    if (!(bucket instanceof final MultiBucketBase multiBucketBase)) {
                        throw new IllegalArgumentException("Aggregate must implement MultiBucketBase");
                    }
                    return multiBucketBase;
                })
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

        return Optional.of(PivotBucket.createOther(keys, OtherBucket.create(otherDocCount), derivedValues, parent, shownBuckets));
    }

    /**
     * Fails closed: if any shown bucket is missing the value or stats needed for this series, the whole series is
     * omitted from the {@code (Other)} bucket rather than derived from a partial, too-large tail.
     */
    private Optional<OtherBucketDerivation.TailInput> tailInput(Pivot pivot,
                                                                 SeriesSpec seriesSpec,
                                                                 long otherDocCount,
                                                                 MultiBucketBase parent,
                                                                 List<MultiBucketBase> shownBuckets,
                                                                 OSGeneratedQueryContext queryContext) {
        if (OtherBucketDerivation.requiresStats(seriesSpec)) {
            final String statsName = statsAggregationName(statsFieldOf(seriesSpec));
            final List<OtherBucketDerivation.Stats> shownStats = shownBuckets.stream()
                    .map(bucket -> readStats(bucket.aggregations(), statsName))
                    .toList();
            if (shownStats.stream().anyMatch(Objects::isNull)) {
                return Optional.empty();
            }
            return Optional.of(new OtherBucketDerivation.TailInput(
                    otherDocCount,
                    null,
                    List.of(),
                    readStats(parent.aggregations(), statsName),
                    shownStats));
        }

        if (!OtherBucketDerivation.requiresSeriesValue(seriesSpec)) {
            return Optional.of(new OtherBucketDerivation.TailInput(otherDocCount, null, List.of(), null, List.of()));
        }

        final String seriesName = queryContext.seriesName(seriesSpec, pivot);
        final List<Double> shownValues = shownBuckets.stream()
                .map(bucket -> readNumeric(bucket.aggregations(), seriesName))
                .toList();
        if (shownValues.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        return Optional.of(new OtherBucketDerivation.TailInput(
                otherDocCount,
                readNumeric(parent.aggregations(), seriesName),
                shownValues,
                null,
                List.of()));
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

    private Stream<PivotBucket> extractTermsBuckets(ImmutableList<String> previousKeys, Function<List<String>, List<String>> reorderKeys, Aggregate termsAggregation) {
        final AggregateVariant rawAggregation = termsAggregation._get();
        if (!(rawAggregation instanceof final TermsAggregateBase<?> terms)) {
            throw new IllegalArgumentException("Aggregate must implement TermsAggregateBase");
        }
        return terms.buckets().array().stream()
                .map(b -> {
                    if (!(b instanceof final MultiBucketBase bucket)) {
                        throw new IllegalArgumentException("Aggregate must implement MultiBucketBase");
                    }
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .addAll(reorderKeys.apply(extractKeys(bucket)))
                            .build();

                    return PivotBucket.create(keys, bucket);
                });
    }

    private List<String> extractKeys(MultiBucketBase bucket) {
        String keys = OSSerializationUtils.getBucketKeyAsString(bucket);
        return splitKeys(keys);
    }

    private ImmutableList<String> splitKeys(String keys) {
        return ImmutableList.copyOf(Splitter.on(KEY_SEPARATOR_CHARACTER).split(keys));
    }
}
