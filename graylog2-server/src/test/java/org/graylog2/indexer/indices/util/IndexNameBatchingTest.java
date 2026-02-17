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
package org.graylog2.indexer.indices.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class IndexNameBatchingTest {

    @Test
    void emptyInputReturnsEmptyList() {
        final List<List<String>> result = IndexNameBatching.partitionByJoinedLength(Collections.emptyList(), 100);
        assertThat(result).isEmpty();
    }

    @Test
    void singleItemFitsInOneBatch() {
        final List<List<String>> result = IndexNameBatching.partitionByJoinedLength(List.of("graylog_0"), 100);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsExactly("graylog_0");
    }

    @Test
    void allItemsFitInOneBatch() {
        final List<String> indices = List.of("index_a", "index_b", "index_c");
        // "index_a,index_b,index_c" = 23 characters
        final List<List<String>> result = IndexNameBatching.partitionByJoinedLength(indices, 100);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsExactly("index_a", "index_b", "index_c");
    }

    @Test
    void splitAcrossMultipleBatches() {
        final List<String> indices = List.of("index_a", "index_b", "index_c", "index_d");
        // Each name is 7 chars. "index_a,index_b" = 15 chars.
        // With maxLength=15, first two fit, then next two go to second batch.
        final List<List<String>> result = IndexNameBatching.partitionByJoinedLength(indices, 15);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly("index_a", "index_b");
        assertThat(result.get(1)).containsExactly("index_c", "index_d");
    }

    @Test
    void singleLongNameExceedingLimitGoesIntoOwnBatch() {
        final String longName = "a".repeat(200);
        final List<String> indices = List.of("short", longName, "also_short");
        final List<List<String>> result = IndexNameBatching.partitionByJoinedLength(indices, 50);
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).containsExactly("short");
        assertThat(result.get(1)).containsExactly(longName);
        assertThat(result.get(2)).containsExactly("also_short");
    }

    @Test
    void exactBoundaryFitsInOneBatch() {
        // "aaa,bbb" = 7 characters
        final List<String> indices = List.of("aaa", "bbb");
        final List<List<String>> result = IndexNameBatching.partitionByJoinedLength(indices, 7);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsExactly("aaa", "bbb");
    }

    @Test
    void oneOverBoundarySplits() {
        // "aaa,bbbb" = 8 characters, limit is 7
        final List<String> indices = List.of("aaa", "bbbb");
        final List<List<String>> result = IndexNameBatching.partitionByJoinedLength(indices, 7);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly("aaa");
        assertThat(result.get(1)).containsExactly("bbbb");
    }

    @Test
    void manyIndicesPartitionedCorrectly() {
        // Simulate realistic scenario with long archive index names
        final List<String> indices = IntStream.range(0, 60)
                .mapToObj(i -> "restored-archive-data-lake-68b002df50e89877d351cb83_" + (1758100000000L + i))
                .toList();

        final List<List<String>> result = IndexNameBatching.partitionByJoinedLength(
                indices, IndexNameBatching.MAX_INDICES_URL_LENGTH);

        // All original indices should be preserved across batches
        final List<String> allFromBatches = result.stream().flatMap(List::stream).toList();
        assertThat(allFromBatches).containsExactlyElementsOf(indices);

        // Each batch's joined length should not exceed the limit
        for (final List<String> batch : result) {
            assertThat(String.join(",", batch).length())
                    .isLessThanOrEqualTo(IndexNameBatching.MAX_INDICES_URL_LENGTH);
        }

        // With 60 indices of ~55 chars each, we expect multiple batches
        assertThat(result.size()).isGreaterThan(1);
    }
}
