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
package org.graylog2.indexer.management;

import org.graylog2.indexer.indices.IndicesAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexHealthServiceTest {
    private final IndexManagementAdapter adapter = mock(IndexManagementAdapter.class);
    private final IndicesAdapter indicesAdapter = mock(IndicesAdapter.class);
    private final IndexHealthService service = new IndexHealthService(adapter, indicesAdapter);

    @Test
    void listsIndicesAndWarmIndicesFromTheAdapter() {
        final List<CatIndex> rows = List.of(new CatIndex("graylog_21", "green", "open", 1, 0, 3L, 900L));
        when(adapter.indices()).thenReturn(rows);
        when(adapter.warmIndices()).thenReturn(Set.of("graylog_20"));

        assertThat(service.indices()).isEqualTo(rows);
        assertThat(service.warmIndices()).containsExactly("graylog_20");
    }

    @Test
    void clearsTheCacheOfExactlyOneIndex() {
        service.clearCache("graylog_3");

        verify(adapter).clearCache("graylog_3");
    }

    @Test
    void opensAnIndexWithoutGraylogsReopenedMarker() {
        service.openIndex("old-index");

        verify(indicesAdapter).openIndex("old-index");
    }
}
