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

import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesBucketOrderingTest {
    private final Values fooPivot = Values.builder().field("foo").build();
    private final Values barPivot = Values.builder().field("bar").build();
    private final Values bazPivot = Values.builder().field("baz").build();

    @Test
    void staysInSameOrderIfNoPivotIsUsedForSort() {
        final List<Values> orderedBuckets = ValuesBucketOrdering.orderBuckets(List.of(fooPivot, barPivot, bazPivot), Collections.emptyList());

        assertThat(orderedBuckets).containsExactly(fooPivot, barPivot, bazPivot);
    }

    @Test
    void staysInSameOrderIfNoPivotSortsAreUsedForSort() {
        final List<Values> orderedBuckets = ValuesBucketOrdering.orderBuckets(List.of(fooPivot, barPivot, bazPivot), List.of(SeriesSort.create("max(took_ms)", SortSpec.Direction.Descending)));

        assertThat(orderedBuckets).containsExactly(fooPivot, barPivot, bazPivot);
    }

    @Test
    void pivotUsedForSortIsPulledToTop() {
        final List<SortSpec> pivotSorts = List.of(PivotSort.create("baz", SortSpec.Direction.Descending));

        final List<Values> orderedBuckets = ValuesBucketOrdering.orderBuckets(List.of(fooPivot, barPivot, bazPivot), pivotSorts);

        assertThat(orderedBuckets).containsExactly(bazPivot, fooPivot, barPivot);
    }

    @Test
    void multiplePivotsUsedForSortArePulledToTop() {
        final List<SortSpec> pivotSorts = List.of(
                PivotSort.create("baz", SortSpec.Direction.Descending),
                PivotSort.create("bar", SortSpec.Direction.Ascending)
        );

        final List<Values> orderedBuckets = ValuesBucketOrdering.orderBuckets(List.of(fooPivot, barPivot, bazPivot), pivotSorts);

        assertThat(orderedBuckets).containsExactly(bazPivot, barPivot, fooPivot);
    }

    @Test
    void reordersKeysBasedOnSortConfiguration() {
        final Function<List<String>, List<String>> reorderKeys = ValuesBucketOrdering.reorderKeysFunction(List.of(fooPivot, barPivot, bazPivot), List.of(
                PivotSort.create("baz", SortSpec.Direction.Descending),
                PivotSort.create("bar", SortSpec.Direction.Ascending)
        ));

        assertThat(reorderKeys.apply(List.of("baz", "bar", "foo"))).containsExactly("foo", "bar", "baz");
    }

    @Test
    void reorderKeysFunctionDoesNotDoAnythingIfNoSortsSpecified() {
        final Function<List<String>, List<String>> reorderKeys = ValuesBucketOrdering.reorderKeysFunction(List.of(fooPivot, barPivot, bazPivot), List.of());

        assertThat(reorderKeys.apply(List.of("baz", "bar", "foo"))).containsExactly("baz", "bar", "foo");
    }
}
