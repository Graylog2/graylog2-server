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

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * A priority queue that has a fixed upper size.
 *
 * @param <E> entry type
 */
public class BoundedPriorityQueue<E> extends PriorityQueue<E> {

    private final int maxSize;

    public BoundedPriorityQueue(int maxSize, Comparator<E> comparator) {
        super(comparator);
        this.maxSize = maxSize;
    }

    public boolean offer(final E e) {
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
        return super.offer(e);
    }
}
