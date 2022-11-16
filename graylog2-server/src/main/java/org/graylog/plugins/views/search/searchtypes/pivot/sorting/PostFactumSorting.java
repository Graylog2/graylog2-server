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
package org.graylog.plugins.views.search.searchtypes.pivot.sorting;

import one.util.streamex.EntryStream;
import org.graylog.plugins.views.search.aggregations.MissingBucketConstants;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PostFactumSorting {

    private static final List<String> DOUBLE_TYPES = List.of("avg", "max", "min", "stddev", "percentile", "sum", "sumofsquares", "variance");
    private static final List<String> LONG_TYPES = List.of("card", "count");

    public Optional<Comparator<PivotResult.Row>> getPostFactumSortingComparator(final Pivot pivot) {

        return pivot.sort().stream()
                .map(sortSpec -> {
                    final SortSpec.Direction sortDirection = sortSpec.direction();
                    if (sortSpec instanceof PivotSort) {
                        final String field = sortSpec.field();
                        final Optional<Integer> rowKeyIndex = getRowKeyIndexForField(pivot, field);
                        if (rowKeyIndex.isPresent()) {
                            return getRowComparatorUsingRowKey(sortDirection, rowKeyIndex.get());

                        }
                    } else if (sortSpec instanceof SeriesSort) {
                        final String field = sortSpec.field();
                        return getRowComparatorUsingRowValue(field, sortDirection);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .reduce(Comparator::thenComparing);
    }

    private Comparator<PivotResult.Row> getRowComparatorUsingRowKey(SortSpec.Direction sortDirection, int rowKeyIndex) {
        Comparator<String> keyComparator;
        if (sortDirection == SortSpec.Direction.Descending) {
            keyComparator = Comparator.nullsLast(Comparator.<String>naturalOrder().reversed());
        } else {
            keyComparator = Comparator.nullsLast(Comparator.naturalOrder());
        }
        return Comparator.comparing(row -> extractKey(row, rowKeyIndex), keyComparator);
    }

    private Comparator<PivotResult.Row> getRowComparatorUsingRowValue(final String field, final SortSpec.Direction sortDirection) {
        final Class<? extends Comparable> valueType = getValueClass(field);

        Comparator<Comparable> valueComparator = Comparator.nullsLast(Comparator.<Comparable>naturalOrder());
        if (sortDirection == SortSpec.Direction.Descending) {
            valueComparator = Comparator.nullsLast(Comparator.<Comparable>naturalOrder().reversed());
        }
        return Comparator.comparing(row -> extractValue(row, field, valueType), valueComparator);

    }

    private Class<? extends Comparable> getValueClass(final String field) {
        Class<? extends Comparable> valueType = String.class;
        if (field.contains("(")) {
            final String functionName = field.substring(0, field.indexOf("("));
            if (DOUBLE_TYPES.contains(functionName)) {
                valueType = Double.class;
            } else if (LONG_TYPES.contains(functionName)) {
                valueType = Long.class;
            }
        }
        return valueType;
    }

    private Comparable extractValue(PivotResult.Row row, String field, Class<? extends Comparable> valueClass) {
        return row.values().stream()
                .filter(v -> v.key().contains(field) && v.key().size() == 1)
                .map(PivotResult.Value::value)
                .findFirst()
                .map(v -> valueClass.cast(v))
                .orElse(null);
    }

    private String extractKey(PivotResult.Row row, int keyIndex) {
        if (row.key().size() <= keyIndex) {
            return null;
        }
        final String extracted = row.key().get(keyIndex);
        if (MissingBucketConstants.MISSING_BUCKET_NAME.equals(extracted)) {
            return null;
        }
        return extracted;
    }

    private Optional<Integer> getRowKeyIndexForField(Pivot pivot, String field) {
        return EntryStream.of(pivot.rowGroups())
                .filterKeyValue((ind, row) -> field.equals(row.field()))
                .keys()
                .findFirst();
    }


}
