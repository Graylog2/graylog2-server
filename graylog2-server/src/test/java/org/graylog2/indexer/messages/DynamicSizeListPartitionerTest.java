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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicSizeListPartitionerTest {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Test
    void worksWithEmptyList() {
        final var partitioner = new DynamicSizeListPartitioner<>(List.of());

        assertThat(partitioner.hasNext()).isFalse();
        assertThatThrownBy(() -> partitioner.nextPartition(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Partition size must be greater than 0");
        assertThat(partitioner.nextPartition(1)).isEmpty();
        assertThat(partitioner.nextPartition(Integer.MAX_VALUE)).isEmpty();
    }

    @Test
    void worksWithSingleElementList() {
        final var list = List.of(23);

        var partitioner = new DynamicSizeListPartitioner<>(list);
        assertThat(partitioner.hasNext()).isTrue();
        assertThat(partitioner.nextPartition(1)).isEqualTo(list);
        assertThat(partitioner.hasNext()).isFalse();
        assertThat(partitioner.nextPartition(1)).isEmpty();
        assertThat(partitioner.nextPartition(Integer.MAX_VALUE)).isEmpty();

        partitioner = new DynamicSizeListPartitioner<>(list);
        assertThat(partitioner.hasNext()).isTrue();
        assertThat(partitioner.nextPartition(Integer.MAX_VALUE)).isEqualTo(list);
        assertThat(partitioner.hasNext()).isFalse();
        assertThat(partitioner.nextPartition(1)).isEmpty();
        assertThat(partitioner.nextPartition(Integer.MAX_VALUE)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000, 1000000})
    void properlyChunksListWithArbitrarySize(int listSize) {
        final var list = createList(listSize);

        var partitioner = new DynamicSizeListPartitioner<>(list);
        final var newList = new ArrayList<Integer>(listSize);

        while (partitioner.hasNext()) {
            final var partitionSize = random.nextInt(1, Integer.MAX_VALUE);
            newList.addAll(partitioner.nextPartition(partitionSize));
        }

        assertThat(newList).hasSize(list.size())
                .isEqualTo(list);
    }

    private List<Integer> createList(int size) {
        return Stream.iterate(0, (previous) -> random.nextInt()).limit(size).toList();
    }
}
