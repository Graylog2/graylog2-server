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
package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.google.common.annotations.VisibleForTesting;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ValuesBucketOrdering {
    private static boolean isGroupingSort(SortSpec sort) {
        return PivotSort.Type.equals(sort.type());
    }

    private static boolean hasGroupingSort(List<SortSpec> sorts) {
        return sorts.stream().anyMatch(ValuesBucketOrdering::isGroupingSort);
    }

    private static boolean needsReordering(List<? extends BucketSpec> bucketSpec, List<SortSpec> sorts) {
        return bucketSpec.size() >= 2 && !sorts.isEmpty() && hasGroupingSort(sorts);
    }

    @VisibleForTesting
    public static <T extends BucketSpec> List<T> orderBuckets(List<T> bucketSpec, List<SortSpec> sorts) {
        if (!needsReordering(bucketSpec, sorts)) {
            return bucketSpec;
        }

        final List<String> sortFields = sorts.stream()
                .filter(ValuesBucketOrdering::isGroupingSort)
                .map(SortSpec::field)
                .collect(Collectors.toList());

        return bucketSpec.stream()
                .sorted(new ValuesBucketComparator<>(sortFields))
                .collect(Collectors.toList());
    }

    public static Function<List<String>, List<String>> reorderKeysFunction(List<BucketSpec> bucketSpecs, List<SortSpec> sorts) {
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
}
