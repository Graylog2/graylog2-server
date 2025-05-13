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
package org.graylog2.indexer.messages;

import java.util.List;

public class DynamicSizeListPartitioner<T> {
    private final List<T> list;
    private int offset;

    public DynamicSizeListPartitioner(List<T> list) {
        this.list = list;
        this.offset = 0;
    }

    public List<T> nextPartition(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Partition size must be greater than 0");
        }
        if (offset >= list.size()) {
            return List.of();
        }
        final var nextOffset = Math.min(offset + size, list.size());
        final var partition = list.subList(offset, nextOffset);
        offset = nextOffset;

        return partition;
    }

    public boolean hasNext() {
        return offset < list.size();
    }
}
