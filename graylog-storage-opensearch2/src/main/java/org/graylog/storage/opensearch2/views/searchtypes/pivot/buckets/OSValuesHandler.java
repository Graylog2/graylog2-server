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
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.ValuesBucketOrdering;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.script.Script;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.BucketOrder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.Filters;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.ParsedFilters;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.PivotBucket;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OSValuesHandler extends OSPivotBucketSpecHandler<Values> {
    private static final String KEY_SEPARATOR_CHARACTER = "\u2E31";
    private static final String KEY_SEPARATOR_PHRASE = " + \"" + KEY_SEPARATOR_CHARACTER + "\" + ";
    private static final String AGG_NAME = "agg";
    public static final BucketOrder DEFAULT_ORDER = BucketOrder.count(false);
    public static final String SORT_HELPER = "sort_helper";

    @Nonnull
    @Override
    public CreatedAggregations<AggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, Values bucketSpec, OSGeneratedQueryContext queryContext, Query query) {
        final List<BucketOrder> ordering = orderListForPivot(pivot, queryContext, DEFAULT_ORDER);
        final int limit = bucketSpec.limit();
        final List<String> orderedBuckets = ValuesBucketOrdering.orderFields(bucketSpec.fields(), pivot.sort());
        final var termsAggregation = createTerms(orderedBuckets, limit);

        applyOrdering(pivot, termsAggregation, ordering, queryContext);

        final FiltersAggregationBuilder filterAggregation = createFilter(name, orderedBuckets, bucketSpec.skipEmptyValues())
                .subAggregation(termsAggregation);
        return CreatedAggregations.create(filterAggregation, termsAggregation, List.of(termsAggregation, filterAggregation));
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

    private TermsAggregationBuilder applyOrdering(Pivot pivot, TermsAggregationBuilder terms, List<BucketOrder> ordering, OSGeneratedQueryContext queryContext) {
        return sortsOnNumericPivotField(pivot, queryContext)
                /* When we sort on a numeric pivot field, we create a metric sub-aggregation for that field, which returns
                the numeric value of it, so that we can sort on it numerically. Any metric aggregation (min/max/avg) will work. */
                .map(pivotSort -> terms
                        .subAggregation(AggregationBuilders.max(SORT_HELPER).field(pivotSort.field()))
                        .order(BucketOrder.aggregation(SORT_HELPER, SortSpec.Direction.Ascending.equals(pivotSort.direction()))))
                .orElseGet(() -> terms
                        .order(ordering.isEmpty() ? List.of(DEFAULT_ORDER) : ordering));
    }

    private Optional<PivotSort> sortsOnNumericPivotField(Pivot pivot, OSGeneratedQueryContext queryContext) {
        return Optional.ofNullable(pivot.sort())
                .filter(sorts -> sorts.size() == 1)
                .map(sorts -> sorts.get(0))
                .filter(sort -> sort instanceof PivotSort)
                .map(sort -> (PivotSort) sort)
                .filter(pivotSort -> queryContext.fieldType(pivot.effectiveStreams(), pivotSort.field())
                        .filter(this::isNumericFieldType)
                        .isPresent());
    }

    private boolean isNumericFieldType(String fieldType) {
        return fieldType.equals("long") || fieldType.equals("double") || fieldType.equals("float");
    }

    @Override
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket initialBucket) {
        var values = (Values) bucketSpecs;
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketsAggregation.Bucket previousBucket = initialBucket.bucket();
        final Function<List<String>, List<String>> reorderKeys = ValuesBucketOrdering.reorderFieldsFunction(bucketSpecs.fields(), pivot.sort());

        final Aggregation aggregation = previousBucket.getAggregations().get(AGG_NAME);
        if (!(aggregation instanceof final ParsedFilters filterAggregation)) {
            // This happens when the other bucket is passed for column value extraction
            return Stream.of(initialBucket);
        }
        final MultiBucketsAggregation termsAggregation = filterAggregation.getBuckets().get(0).getAggregations().get(AGG_NAME);
        if (values.skipEmptyValues()) {
            return extractTermsBuckets(previousKeys, reorderKeys, termsAggregation, values.skipEmptyValues());
        } else {
            final Filters.Bucket otherBucket = filterAggregation.getBuckets().get(1);

            final Stream<PivotBucket> bucketStream = extractTermsBuckets(previousKeys, reorderKeys, termsAggregation, values.skipEmptyValues());

            if (otherBucket.getDocCount() > 0) {
                final MultiBucketsAggregation otherTermsAggregations = otherBucket.getAggregations().get(AGG_NAME);
                final var otherStream = extractTermsBuckets(previousKeys, reorderKeys, otherTermsAggregations, values.skipEmptyValues());
                return Stream.concat(bucketStream, otherStream);
            } else {
                return bucketStream;
            }
        }
    }

    private Stream<PivotBucket> extractTermsBuckets(ImmutableList<String> previousKeys, Function<List<String>, List<String>> reorderKeys, MultiBucketsAggregation termsAggregation, boolean skipEmptyValues) {
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
