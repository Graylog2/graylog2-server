package org.graylog2.database;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class SliceablePaginatedList<E> extends PaginatedList<E> {
    private final List<Slice> slices;

    public SliceablePaginatedList(@Nonnull List<E> delegate, int total, int page, int perPage, Long grandTotal, List<Slice> slices) {
        super(delegate, total, page, perPage, grandTotal);
        this.slices = slices;
    }

    public Optional<List<Slice>> slices() {
        return Optional.ofNullable(slices);
    }
}
