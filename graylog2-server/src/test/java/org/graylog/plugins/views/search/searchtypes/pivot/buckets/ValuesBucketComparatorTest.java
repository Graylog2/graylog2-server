package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesBucketComparatorTest {
    private final Values fooPivot = Values.builder().field("foo").build();
    private final Values barPivot = Values.builder().field("bar").build();
    private final Values bazPivot = Values.builder().field("baz").build();

    @Test
    void returnZeroIfSortlistIsEmpty() {
        final ValuesBucketComparator<BucketSpec> comparator = new ValuesBucketComparator<>(Collections.emptyList());

        assertThat(comparator.compare(fooPivot, barPivot)).isZero();
        assertThat(comparator.compare(barPivot, fooPivot)).isZero();
        assertThat(comparator.compare(fooPivot, fooPivot)).isZero();

        assertThat(Stream.of(bazPivot, fooPivot, barPivot)
                .sorted(comparator)
                .collect(Collectors.toList())).containsExactly(bazPivot, fooPivot, barPivot);
        assertThat(Stream.of(fooPivot, barPivot, bazPivot)
                .sorted(comparator)
                .collect(Collectors.toList())).containsExactly(fooPivot, barPivot, bazPivot);
    }

    @Test
    void fieldOnSortlistIsHigher() {
        final ValuesBucketComparator<BucketSpec> comparator = new ValuesBucketComparator<>(List.of("foo"));
        assertThat(comparator.compare(fooPivot, barPivot)).isEqualTo(-1);
        assertThat(comparator.compare(barPivot, fooPivot)).isOne();
        assertThat(comparator.compare(barPivot, bazPivot)).isZero();

        assertThat(Stream.of(bazPivot, fooPivot, barPivot)
                .sorted(comparator)
                .collect(Collectors.toList())).containsExactly(fooPivot, bazPivot, barPivot);
    }

    @Test
    void sortInOrderIfBothFieldsAreOnSortlist() {
        final ValuesBucketComparator<BucketSpec> comparator = new ValuesBucketComparator<>(List.of("bar", "foo"));
        assertThat(comparator.compare(fooPivot, barPivot)).isOne();
        assertThat(comparator.compare(fooPivot, bazPivot)).isEqualTo(-1);
        assertThat(comparator.compare(barPivot, fooPivot)).isEqualTo(-1);
        assertThat(comparator.compare(barPivot, bazPivot)).isEqualTo(-1);

        assertThat(Stream.of(bazPivot, fooPivot, barPivot)
                .sorted(comparator)
                .collect(Collectors.toList())).containsExactly(barPivot, fooPivot, bazPivot);
    }

    @Test
    void sortInOrderIfAllFieldsAreOnSortlist() {
        final ValuesBucketComparator<BucketSpec> comparator = new ValuesBucketComparator<>(List.of("baz", "bar", "foo"));
        assertThat(comparator.compare(fooPivot, barPivot)).isOne();
        assertThat(comparator.compare(fooPivot, bazPivot)).isOne();
        assertThat(comparator.compare(barPivot, fooPivot)).isEqualTo(-1);
        assertThat(comparator.compare(barPivot, bazPivot)).isOne();

        assertThat(Stream.of(fooPivot, barPivot, bazPivot)
                .sorted(comparator)
                .collect(Collectors.toList())).containsExactly(bazPivot, barPivot, fooPivot);
    }
}
