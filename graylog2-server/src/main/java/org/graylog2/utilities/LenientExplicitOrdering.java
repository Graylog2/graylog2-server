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

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import java.util.List;
import java.util.Map;

/**
 * An ordering that compares objects according to a given order, sorting unknown elements by their natural comparison or last.
 */
public class LenientExplicitOrdering<T> extends Ordering<T> {
    private final Map<T, Integer> idxMap;

    public LenientExplicitOrdering(List<T> order) {
        this.idxMap = Maps.newHashMapWithExpectedSize(order.size());
        int idx = 0;
        for (T s : order) {
            idxMap.put(s, idx);
            idx++;
        }
    }

    @Override
    public int compare(T left, T right) {
        final Integer leftIdx = idxMap.get(left);
        final Integer rightIdx = idxMap.get(right);

        if (leftIdx != null && rightIdx != null) {
            //noinspection SuspiciousNameCombination
            return Integer.compare(leftIdx, rightIdx);
        }
        if (left instanceof Comparable && leftIdx == null && rightIdx == null) {
            //noinspection unchecked
            return ((Comparable) left).compareTo(right);
        }
        if (leftIdx == null) {
            return -1;
        }
        return 1;
    }
}
