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
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.ValuesBucketOrdering;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.NamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.PivotBucket;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.AggregateVariant;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.DoubleTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.FiltersAggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersBucket;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.TermsAggregateBase;
import org.opensearch.client.opensearch._types.aggregations.TermsAggregation;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.ExistsQuery;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OSValuesHandler extends OSPivotBucketSpecHandler<Values> {
    private static final String KEY_SEPARATOR_CHARACTER = "\u2E31";
    private static final String KEY_SEPARATOR_PHRASE = " + \"" + KEY_SEPARATOR_CHARACTER + "\" + ";
    private static final String AGG_NAME = "agg";
    public static final BucketOrder DEFAULT_ORDER = BucketOrder.count(SortOrder.Desc);
    public static final String SORT_HELPER = "sort_helper";

    @Nonnull
    @Override
    public CreatedAggregations<NamedAggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, Values bucketSpec, OSGeneratedQueryContext queryContext, Query query) {
        final var ordering = orderListForPivot(pivot, queryContext, DEFAULT_ORDER, query);
        final int limit = bucketSpec.limit();
        final List<String> orderedBuckets = ValuesBucketOrdering.orderFields(bucketSpec.fields(), pivot.sort());
        final TermsAggregation.Builder termsAggregationBuilder = createTerms(orderedBuckets, limit);

        termsAggregationBuilder.order(mapOrders(ordering.orders()));
        NamedAggregationBuilder termsAggregation = new NamedAggregationBuilder(
                AGG_NAME,
                Aggregation.builder()
                        .terms(termsAggregationBuilder.build())
                        .aggregations(ordering.sortingAggregations())
        );

        final FiltersAggregation.Builder filterAggregationBuilder = createFilter(orderedBuckets, bucketSpec.skipEmptyValues());
        final NamedAggregationBuilder filterAggregation = new NamedAggregationBuilder(
                name,
                Aggregation.builder()
                        .filters(filterAggregationBuilder.build())
                        .aggregations(termsAggregation.name(), termsAggregation.aggregationBuilder().build())
        );
        return CreatedAggregations.create(filterAggregation, termsAggregation, List.of(termsAggregation, filterAggregation));
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
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket initialBucket) {
        var values = (Values) bucketSpecs;
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketBase previousBucket = initialBucket.bucket();
        final Function<List<String>, List<String>> reorderKeys = ValuesBucketOrdering.reorderFieldsFunction(bucketSpecs.fields(), pivot.sort());

        final Aggregate aggregation = previousBucket.aggregations().get(AGG_NAME);
        if (!(aggregation.isFilters())) {
            // This happens when the other bucket is passed for column value extraction
            return Stream.of(initialBucket);
        }
        Aggregate termsAggregation = aggregation.filters().buckets().array().getFirst().aggregations().get(AGG_NAME);

        if (values.skipEmptyValues()) {
            return extractTermsBuckets(previousKeys, reorderKeys, termsAggregation, values.skipEmptyValues());
        } else {
            FiltersBucket otherBucket = aggregation.filters().buckets().array().get(1);

            final Stream<PivotBucket> bucketStream = extractTermsBuckets(previousKeys, reorderKeys, termsAggregation, values.skipEmptyValues());

            if (otherBucket.docCount() > 0) {
                final Aggregate otherTermsAggregations = otherBucket.aggregations().get(AGG_NAME);
                final var otherStream = extractTermsBuckets(previousKeys, reorderKeys, otherTermsAggregations, values.skipEmptyValues());
                return Stream.concat(bucketStream, otherStream);
            } else {
                return bucketStream;
            }
            }
    }

    private Stream<PivotBucket> extractTermsBuckets(ImmutableList<String> previousKeys, Function<List<String>, List<String>> reorderKeys, Aggregate termsAggregation, boolean skipEmptyValues) {
        AggregateVariant rawAggregation = termsAggregation._get();
        if (!(rawAggregation instanceof TermsAggregateBase<?> terms)) {
            throw new IllegalArgumentException("Aggregate must implement TermsAggregateBase");
        }
        return terms.buckets().array().stream()
                .map(b -> {
                    if (!(b instanceof MultiBucketBase bucket)) {
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
        String keys = switch (bucket) {
            case StringTermsBucket sterms -> sterms.key();
            case DoubleTermsBucket dterms -> dterms.keyAsString();
            case LongTermsBucket lterms -> lterms.keyAsString();
            //TODO: do we need to consider more types?
            default -> "";
        };
        return splitKeys(keys);
    }

    private ImmutableList<String> splitKeys(String keys) {
        return ImmutableList.copyOf(Splitter.on(KEY_SEPARATOR_CHARACTER).split(keys));
    }
}
