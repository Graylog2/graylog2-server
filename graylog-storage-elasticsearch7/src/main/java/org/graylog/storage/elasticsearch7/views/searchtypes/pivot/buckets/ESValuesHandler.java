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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.aggregations.MissingBucketConstants;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ESValuesHandler extends ESPivotBucketSpecHandler<Values> {
    private static final String KEY_SEPARATOR_CHARACTER = "\u2E31";
    private static final String KEY_SEPARATOR_PHRASE = " + \"" + KEY_SEPARATOR_CHARACTER + "\" + ";
    private static final String AGG_NAME = "agg";
    private static final ImmutableList<String> MISSING_BUCKET_KEYS = ImmutableList.of(MissingBucketConstants.MISSING_BUCKET_NAME);
    private static final BucketOrder defaultOrder = BucketOrder.count(false);

    @Nonnull
    @Override
    public CreatedAggregations<AggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, List<Values> bucketSpec, ESGeneratedQueryContext queryContext, Query query) {
        final List<BucketOrder> ordering = orderListForPivot(pivot, queryContext, defaultOrder);
        final int limit = extractLimit(direction, pivot).orElse(Values.DEFAULT_LIMIT);
        final List<Values> orderedBuckets = orderBuckets(bucketSpec, pivot.sort());
        final AggregationBuilder termsAggregation = createTerms(orderedBuckets, ordering, limit);
        final FiltersAggregationBuilder filterAggregation = createFilter(name, orderedBuckets)
                .subAggregation(termsAggregation);

        return CreatedAggregations.create(filterAggregation, termsAggregation, List.of(termsAggregation, filterAggregation));
    }

    private boolean isGroupingSort(SortSpec sort) {
        return "pivot".equals(sort.type());
    }

    private boolean hasGroupingSort(List<SortSpec> sorts) {
        return sorts.stream().anyMatch(this::isGroupingSort);
    }

    private boolean needsReordering(List<? extends BucketSpec> bucketSpec, List<SortSpec> sorts) {
        return bucketSpec.size() >= 2 && !sorts.isEmpty() && hasGroupingSort(sorts);
    }

    @VisibleForTesting
    <T extends BucketSpec> List<T> orderBuckets(List<T> bucketSpec, List<SortSpec> sorts) {
        if (!needsReordering(bucketSpec, sorts)) {
            return bucketSpec;
        }

        final List<String> sortFields = sorts.stream()
                .filter(this::isGroupingSort)
                .map(SortSpec::field)
                .collect(Collectors.toList());

        return bucketSpec.stream()
                .sorted((s1, s2) -> {
                    int s1Index = sortFields.indexOf(s1.field());
                    int s2Index = sortFields.indexOf(s2.field());
                    if (s1Index == s2Index) {
                        return 0;
                    }
                    if (s1Index == -1) {
                        return 1;
                    }
                    if (s2Index == -1) {
                        return -1;
                    }
                    return Integer.compare(s1Index, s2Index);
                })
                .collect(Collectors.toList());
    }

    private Function<List<String>, List<String>> reorderKeysFunction(List<BucketSpec> bucketSpecs, List<SortSpec> sorts) {
        if (!needsReordering(bucketSpecs, sorts)) {
            return Function.identity();
        }

        final List<BucketSpec> orderedBuckets = orderBuckets(bucketSpecs, sorts);
        final Map<Integer, Integer> mapping = IntStream.range(0, bucketSpecs.size())
                .boxed()
                .collect(Collectors.toMap(Function.identity(), i -> orderedBuckets.indexOf(bucketSpecs.get(i))));

        return (keys) -> IntStream.range(0, bucketSpecs.size())
                .boxed()
                .map(i -> keys.get(mapping.get(i)))
                .collect(Collectors.toList());
    }

    private Optional<Integer> extractLimit(Direction direction, Pivot pivot) {
        return switch (direction) {
            case Row -> pivot.rowLimit();
            case Column -> pivot.columnLimit();
        };
    }

    private FiltersAggregationBuilder createFilter(String name, List<Values> bucketSpecs) {
        final BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        bucketSpecs.stream()
                .map(Values::field)
                .map(QueryBuilders::existsQuery)
                .forEach(queryBuilder::filter);
        return AggregationBuilders.filters(name, queryBuilder)
                .otherBucket(true);
    }


    private AggregationBuilder createTerms(List<Values> valueBuckets, List<BucketOrder> ordering, int limit) {
        return valueBuckets.size() > 1
                ? createScriptedTerms(valueBuckets, ordering, limit)
                : createSimpleTerms(valueBuckets.get(0), ordering, limit);
    }

    private TermsAggregationBuilder createSimpleTerms(Values value, List<BucketOrder> ordering, int limit) {
        return AggregationBuilders.terms(AGG_NAME)
                .field(value.field())
                .order(ordering)
                .size(limit);
    }

    private TermsAggregationBuilder createScriptedTerms(List<? extends BucketSpec> buckets, List<BucketOrder> ordering, int limit) {
        return AggregationBuilders.terms(AGG_NAME)
                .script(scriptForPivots(buckets))
                .size(limit)
                .order(ordering);
    }

    private Script scriptForPivots(Collection<? extends BucketSpec> pivots) {
        final String scriptSource = Joiner.on(KEY_SEPARATOR_PHRASE).join(pivots.stream()
                .map(bucket -> """
                        String.valueOf((doc.containsKey('%1$s') && doc['%1$s'].size() > 0) ? doc['%1$s'].value : "%2$s")
                        """.formatted(bucket.field(), MissingBucketConstants.MISSING_BUCKET_NAME))
                .collect(Collectors.toList()));
        return new Script(scriptSource);
    }

    @Override
    public Stream<PivotBucket> extractBuckets(Pivot pivot, List<BucketSpec> bucketSpecs, PivotBucket initialBucket) {
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketsAggregation.Bucket previousBucket = initialBucket.bucket();
        final Aggregation aggregation = previousBucket.getAggregations().get(AGG_NAME);
        if (!(aggregation instanceof final ParsedFilters filterAggregation)) {
            // This happens when the other bucket is passed for column value extraction
            return Stream.of(initialBucket);
        }
        final MultiBucketsAggregation termsAggregation = filterAggregation.getBuckets().get(0).getAggregations().get(AGG_NAME);
        final Filters.Bucket otherBucket = filterAggregation.getBuckets().get(1);

        final Function<List<String>, List<String>> reorderKeys = reorderKeysFunction(bucketSpecs, pivot.sort());
        final Stream<PivotBucket> bucketStream = termsAggregation.getBuckets()
                .stream()
                .map(bucket -> {
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .addAll(reorderKeys.apply(splitKeys(bucket.getKeyAsString())))
                            .build();

                    return PivotBucket.create(keys, bucket);
                });

        return otherBucket.getDocCount() > 0
                ? Stream.concat(bucketStream, Stream.of(PivotBucket.create(MISSING_BUCKET_KEYS, otherBucket)))
                : bucketStream;
    }

    private ImmutableList<String> splitKeys(String keys) {
        return ImmutableList.copyOf(Splitter.on(KEY_SEPARATOR_CHARACTER).split(keys));
    }
}
