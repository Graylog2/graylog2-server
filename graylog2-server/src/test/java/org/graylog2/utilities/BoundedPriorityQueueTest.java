package org.graylog2.utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

class BoundedPriorityQueueTest {

    @Test
    void add() {
        // the queue behaves like a standard PriorityQueue, but should never grow past the bound we specify, so this is what we test here
        // not the rest of the behavior
        final BoundedPriorityQueue<Integer> queue = new BoundedPriorityQueue<Integer>(10, Comparator.naturalOrder());

        for (int i = 0; i < 42; i++) {
            queue.add(i);
        }
        Assertions.assertEquals(10, queue.size());

    }
}
