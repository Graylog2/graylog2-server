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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets;

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
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.script.Script;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.BucketOrder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.Filters;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.PivotBucket;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ESValuesHandler extends ESPivotBucketSpecHandler<Values> {
    private static final String KEY_SEPARATOR_CHARACTER = "\u2E31";
    private static final String KEY_SEPARATOR_PHRASE = " + \"" + KEY_SEPARATOR_CHARACTER + "\" + ";
    private static final String AGG_NAME = "agg";
    public static final BucketOrder DEFAULT_ORDER = BucketOrder.count(false);
    public static final String SORT_HELPER = "sort_helper";

    @Nonnull
    @Override
    public CreatedAggregations<AggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, Values bucketSpec, ESGeneratedQueryContext queryContext, Query query) {
        final var ordering = orderListForPivot(pivot, queryContext, DEFAULT_ORDER, query);
        final int limit = bucketSpec.limit();
        final List<String> orderedBuckets = ValuesBucketOrdering.orderFields(bucketSpec.fields(), pivot.sort());
        final var termsAggregation = createTerms(orderedBuckets, limit);

        termsAggregation.order(ordering.orders());
        ordering.sortingAggregations().forEach(termsAggregation::subAggregation);

        final FiltersAggregationBuilder filterAggregation = createFilter(name, orderedBuckets, bucketSpec.skipEmptyValues())
                .subAggregation(termsAggregation);
        return CreatedAggregations.create(filterAggregation, termsAggregation, List.of(termsAggregation, filterAggregation));
    }

    private FiltersAggregationBuilder createFilter(String name, List<String> fields, boolean skipEmptyValues) {
        final BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        fields.stream()
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

    private TermsAggregationBuilder applyOrdering(Pivot pivot, TermsAggregationBuilder terms, List<BucketOrder> ordering, ESGeneratedQueryContext queryContext) {
        return sortsOnNumericPivotField(pivot, queryContext)
                /* When we sort on a numeric pivot field, we create a metric sub-aggregation for that field, which returns
                the numeric value of it, so that we can sort on it numerically. Any metric aggregation (min/max/avg) will work. */
                .map(pivotSort -> terms
                        .subAggregation(AggregationBuilders.max(SORT_HELPER).field(pivotSort.field()))
                        .order(BucketOrder.aggregation(SORT_HELPER, SortSpec.Direction.Ascending.equals(pivotSort.direction()))))
                .orElseGet(() -> terms
                        .order(ordering.isEmpty() ? List.of(DEFAULT_ORDER) : ordering));
    }

    private Optional<PivotSort> sortsOnNumericPivotField(Pivot pivot, ESGeneratedQueryContext queryContext) {
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
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpec, PivotBucket initialBucket) {
        var values = (Values) bucketSpec;
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketsAggregation.Bucket previousBucket = initialBucket.bucket();
        final Function<List<String>, List<String>> reorderKeys = ValuesBucketOrdering.reorderFieldsFunction(bucketSpec.fields(), pivot.sort());

        final Aggregation aggregation = previousBucket.getAggregations().get(AGG_NAME);
        if (!(aggregation instanceof final ParsedFilters filterAggregation)) {
            // This happens when the other bucket is passed for column value extraction
            return Stream.of(initialBucket);
        }
        final MultiBucketsAggregation termsAggregation = filterAggregation.getBuckets().get(0).getAggregations().get(AGG_NAME);
        if (values.skipEmptyValues()) {
            return extractTermsBuckets(previousKeys, reorderKeys, termsAggregation);
        } else {

            final Filters.Bucket otherBucket = filterAggregation.getBuckets().get(1);

            final Stream<PivotBucket> bucketStream = extractTermsBuckets(previousKeys, reorderKeys, termsAggregation);

            if (otherBucket.getDocCount() > 0) {
                final MultiBucketsAggregation otherTermsAggregations = otherBucket.getAggregations().get(AGG_NAME);
                final var otherStream = extractTermsBuckets(previousKeys, reorderKeys, otherTermsAggregations);
                return Stream.concat(bucketStream, otherStream);
            } else {
                return bucketStream;
            }
        }
    }

    private Stream<PivotBucket> extractTermsBuckets(ImmutableList<String> previousKeys, Function<List<String>, List<String>> reorderKeys, MultiBucketsAggregation termsAggregation) {
        return termsAggregation.getBuckets()
                .stream()
                .map(bucket -> {
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .addAll(reorderKeys.apply(splitKeys(bucket.getKeyAsString())))
                            .build();

                    return PivotBucket.create(keys, bucket, false);
                });
    }

    private ImmutableList<String> splitKeys(String keys) {
        return ImmutableList.copyOf(Splitter.on(KEY_SEPARATOR_CHARACTER).split(keys));
    }
}
