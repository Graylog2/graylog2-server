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
package org.graylog2.shared.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Distributes a total size proportionally across items based on individual weights.
 * The last item receives the remainder to ensure the total is preserved exactly.
 */
public final class InputMessageSizeDistributor {

    private InputMessageSizeDistributor() {
    }

    /**
     * Distributes {@code totalSize} proportionally across items based on their {@code weights}.
     *
     * @param totalSize the total size to distribute
     * @param weights   the weight of each item (e.g. serialized size of each record)
     * @return a list of distributed sizes, same length as {@code weights}
     */
    public static List<Long> distribute(final long totalSize, final List<Long> weights) {
        final int count = weights.size();

        if (count == 0) {
            return Collections.emptyList();
        }

        final long totalWeight = weights.stream().mapToLong(Long::longValue).sum();

        final List<Long> result = new ArrayList<>(count);
        long assignedSize = 0L;
        for (int i = 0; i < count; i++) {
            final long size;
            if (i == count - 1) {
                size = totalSize - assignedSize;
            } else if (totalWeight > 0) {
                size = totalSize * weights.get(i) / totalWeight;
            } else {
                size = totalSize / count;
            }
            assignedSize += size;
            result.add(size);
        }

        return result;
    }
}
