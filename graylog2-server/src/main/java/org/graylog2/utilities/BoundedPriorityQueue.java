package org.graylog2.utilities;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * A priority queue that has a fixed upper size.
 *
 * @param <E> entry type
 */
public class FixedSizePriorityQueue<E> extends PriorityQueue<E> {

    private final int maxSize;

    public FixedSizePriorityQueue(int maxSize, Comparator<E> comparator) {
        super(comparator);
        this.maxSize = maxSize;
    }

    public boolean add(final E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        if (maxSize <= size()) {
            final E firstElm = peek();
            if (comparator().compare(e, firstElm) < 1) {
                return false;
            } else {
                poll();
            }
        }
        return offer(e);
    }
}
