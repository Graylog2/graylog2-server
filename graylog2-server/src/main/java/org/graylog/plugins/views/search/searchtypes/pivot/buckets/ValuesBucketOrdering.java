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

    private static boolean needsReorderingFields(List<String> fields, List<SortSpec> sorts) {
        return fields.size() >= 2 && !sorts.isEmpty() && hasGroupingSort(sorts);
    }

    public static List<String> orderFields(List<String> fields, List<SortSpec> sorts) {
        if (!needsReorderingFields(fields, sorts)) {
            return fields;
        }

        final List<String> sortFields = sorts.stream()
                .filter(ValuesBucketOrdering::isGroupingSort)
                .map(SortSpec::field)
                .collect(Collectors.toList());

        return fields.stream()
                .sorted(new FieldsSortingComparator(sortFields))
                .collect(Collectors.toList());
    }

    public static Function<List<String>, List<String>> reorderFieldsFunction(List<String> fields, List<SortSpec> sorts) {
        if (!needsReorderingFields(fields, sorts)) {
            return Function.identity();
        }

        final List<String> orderedBuckets = orderFields(fields, sorts);
        final Map<Integer, Integer> mapping = IntStream.range(0, fields.size())
                .boxed()
                .collect(Collectors.toMap(Function.identity(), i -> orderedBuckets.indexOf(fields.get(i))));

        return (keys) -> IntStream.range(0, fields.size())
                .boxed()
                .map(i -> keys.get(mapping.get(i)))
                .collect(Collectors.toList());
    }
}
