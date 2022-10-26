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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.BucketOrder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.Terms;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OSValuesHandler extends OSPivotBucketSpecHandler<Values, Terms> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Values valuesSpec, OSGeneratedQueryContext OSGeneratedQueryContext, Query query) {
        final List<BucketOrder> ordering = orderListForPivot(pivot, valuesSpec, OSGeneratedQueryContext);
        final TermsAggregationBuilder builder = AggregationBuilders.terms(name)
                .minDocCount(1)
                .field(valuesSpec.field())
                .order(ordering.isEmpty() ? Collections.singletonList(BucketOrder.count(false)) : ordering)
                .size(valuesSpec.limit());
        record(OSGeneratedQueryContext, pivot, valuesSpec, name, Terms.class);
        return Optional.of(builder);
    }

    private List<BucketOrder> orderListForPivot(Pivot pivot, Values valuesSpec, OSGeneratedQueryContext OSGeneratedQueryContext) {
        return pivot.sort()
                .stream()
                .map(sortSpec -> {
                    if (sortSpec instanceof PivotSort && valuesSpec.field().equals(sortSpec.field())) {
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
                                    return BucketOrder.aggregation(OSGeneratedQueryContext.seriesName(seriesSpec, pivot), sortSpec.direction().equals(SortSpec.Direction.Ascending));
                                })
                                .orElse(null);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Stream<Bucket> doHandleResult(Values bucketSpec,
                                         Terms termsAggregation) {
        return termsAggregation.getBuckets().stream()
                .map(entry -> Bucket.create(entry.getKeyAsString(), entry));
    }
}
