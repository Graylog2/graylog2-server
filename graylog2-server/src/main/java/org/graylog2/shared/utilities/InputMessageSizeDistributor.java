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
     * @return an array of distributed sizes, same length as {@code weights}
     */
    public static long[] distribute(final long totalSize, final long[] weights) {
        final int count = weights.length;
        final long[] result = new long[count];

        if (count == 0) {
            return result;
        }

        long totalWeight = 0L;
        for (final long weight : weights) {
            totalWeight += weight;
        }

        long assignedSize = 0L;
        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                result[i] = totalSize - assignedSize;
            } else if (totalWeight > 0) {
                // Can overflow if totalSize * weight exceeds Long.MAX_VALUE (~9.2 EB), not a real-world concern
                result[i] = totalSize * weights[i] / totalWeight;
            }
            assignedSize += result[i];
        }

        return result;
    }
}
