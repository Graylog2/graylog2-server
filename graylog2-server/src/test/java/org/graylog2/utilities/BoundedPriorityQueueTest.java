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
