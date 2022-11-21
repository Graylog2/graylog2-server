package org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets;

import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OSValuesHandlerTest {
    private final OSValuesHandler osValuesHandler = new OSValuesHandler(SearchVersion.opensearch("2.2.0"));
    private final Values fooPivot = Values.builder().field("foo").build();
    private final Values barPivot = Values.builder().field("bar").build();
    private final Values bazPivot = Values.builder().field("baz").build();

    @Test
    void staysInSameOrderIfNoPivotIsUsedForSort() {
        final List<Values> orderedBuckets = osValuesHandler.orderBuckets(List.of(fooPivot, barPivot, bazPivot), Collections.emptyList());

        assertThat(orderedBuckets).containsExactly(fooPivot, barPivot, bazPivot);
    }

    @Test
    void pivotUsedForSortIsPulledToTop() {
        final List<SortSpec> pivotSorts = List.of(PivotSort.create("pivot", "baz", SortSpec.Direction.Descending));

        final List<Values> orderedBuckets = osValuesHandler.orderBuckets(List.of(fooPivot, barPivot, bazPivot), pivotSorts);

        assertThat(orderedBuckets).containsExactly(bazPivot, fooPivot, barPivot);
    }

    @Test
    void multiplePivotsUsedForSortArePulledToTop() {
        final List<SortSpec> pivotSorts = List.of(
                PivotSort.create("pivot", "baz", SortSpec.Direction.Descending),
                PivotSort.create("pivot", "bar", SortSpec.Direction.Ascending)
        );

        final List<Values> orderedBuckets = osValuesHandler.orderBuckets(List.of(fooPivot, barPivot, bazPivot), pivotSorts);

        assertThat(orderedBuckets).containsExactly(bazPivot, barPivot, fooPivot);
    }
}
