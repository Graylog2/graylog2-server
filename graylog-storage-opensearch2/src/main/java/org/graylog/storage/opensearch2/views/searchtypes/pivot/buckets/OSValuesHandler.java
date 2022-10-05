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
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.shaded.opensearch2.org.opensearch.script.Script;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.BucketOrder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.Terms;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.support.MultiTermsValuesSourceConfig;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog2.storage.DetectedSearchVersion;
import org.graylog2.storage.SearchVersion;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OSValuesHandler extends OSPivotBucketSpecHandler<Values, Terms> {
    private static final String KEY_SEPARATOR_CHARACTER = "\u2E31";
    private static final String KEY_SEPARATOR_PHRASE = " + \"" + KEY_SEPARATOR_CHARACTER + "\" + ";
    private final boolean supportsMultiTerms;
    private static final String AGG_NAME = "agg";

    @Inject
    public OSValuesHandler(@DetectedSearchVersion SearchVersion version) {
        this.supportsMultiTerms = version.satisfies(SearchVersion.Distribution.OPENSEARCH, ">=2.2.0");
    }

    @Nonnull
    @Override
    public Optional<Tuple2<AggregationBuilder, AggregationBuilder>> doCreateAggregation(String name, Pivot pivot, List<Values> bucketSpec, OSGeneratedQueryContext queryContext, Query query) {
        final List<BucketOrder> ordering = orderListForPivot(pivot, queryContext);
        final int limit = bucketSpec.stream()
                .map(Values::limit)
                .max(Integer::compare)
                .orElse(Values.DEFAULT_LIMIT);
        final AggregationBuilder builder = createTerms(bucketSpec, ordering, limit);
        return Optional.of(new Tuple2<>(builder, builder));
    }

    private AggregationBuilder createTerms(List<Values> valueBuckets, List<BucketOrder> ordering, int limit) {
        return valueBuckets.size() > 1
                ? supportsMultiTerms
                ? createMultiTerms(valueBuckets, ordering, limit)
                : createScriptedTerms(valueBuckets, ordering, limit)
                : createSimpleTerms(valueBuckets.get(0), ordering, limit);
    }

    private AggregationBuilder createSimpleTerms(Values values, List<BucketOrder> ordering, int limit) {
        return AggregationBuilders.terms(AGG_NAME)
                .field(values.field())
                .order(ordering)
                .size(limit);
    }

    private AggregationBuilder createMultiTerms(List<Values> valueBuckets, List<BucketOrder> ordering, int limit) {
        return AggregationBuilders.multiTerms(AGG_NAME)
                .terms(valueBuckets.stream()
                        .map(value -> new MultiTermsValuesSourceConfig.Builder()
                                .setFieldName(value.field())
                                .build())
                        .collect(Collectors.toList()))
                .order(ordering)
                .size(limit);
    }

    private TermsAggregationBuilder createScriptedTerms(List<? extends BucketSpec> buckets, List<BucketOrder> ordering, int limit) {
        return AggregationBuilders.terms(AGG_NAME)
                .script(scriptForPivots(buckets))
                .size(limit)
                .order(ordering.isEmpty() ? List.of(BucketOrder.count(false)) : ordering);
    }

    private Script scriptForPivots(Collection<? extends BucketSpec> pivots) {
        final String scriptSource = Joiner.on(KEY_SEPARATOR_PHRASE).join(pivots.stream()
                .map(bucket -> "doc['" + bucket.field() + "'].value")
                .collect(Collectors.toList()));
        return new Script(scriptSource);
    }

    @Override
    public Stream<Tuple2<ImmutableList<String>, MultiBucketsAggregation.Bucket>> extractBuckets(List<BucketSpec> bucketSpecs,
                                                                                                Tuple2<ImmutableList<String>, MultiBucketsAggregation.Bucket> tuple) {
        final ImmutableList<String> previousKeys = tuple.v1();
        final MultiBucketsAggregation.Bucket previousBucket = tuple.v2();

        final MultiBucketsAggregation aggregation = previousBucket.getAggregations().get(AGG_NAME);
        return aggregation.getBuckets().stream()
                .map(bucket -> {
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .addAll(extractKeys(bucket))
                            .build();

                    return new Tuple2<>(keys, bucket);
                });

    }

    private Iterable<String> extractKeys(MultiBucketsAggregation.Bucket bucket) {
        return supportsMultiTerms ? extractMultiTermsKeys(bucket) : splitKeys(bucket.getKeyAsString());
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

    private ImmutableList<String> splitKeys(String keys) {
        return ImmutableList.copyOf(Splitter.on(KEY_SEPARATOR_CHARACTER).split(keys));
    }
}
