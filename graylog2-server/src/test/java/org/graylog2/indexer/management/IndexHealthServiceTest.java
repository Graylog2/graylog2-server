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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexHealthServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IndexManagementAdapter adapter = mock(IndexManagementAdapter.class);
    private final IndexHealthService service = new IndexHealthService(Optional.of(adapter));

    @Test
    void listsEveryIndexIncludingHiddenAndClosedOnes() throws Exception {
        when(adapter.request(eq("GET"), eq("/_cat/indices"), anyMap(), isNull(), anyString())).thenReturn(objectMapper.readTree("""
                [{"index":"graylog_21","health":"green","status":"open","pri":"1","rep":"0","docs.count":"3","store.size":"900"},
                 {"index":"graylog_5","health":null,"status":"close","pri":"1","rep":"0","docs.count":null,"store.size":null}]"""));

        assertThat(service.catIndices()).hasValueSatisfying(rows -> assertThat(rows).containsExactly(
                new CatIndex("graylog_21", "green", "open", 1, 0, 3L, 900L),
                new CatIndex("graylog_5", null, "close", 1, 0, null, null)));
        verify(adapter).request(eq("GET"), eq("/_cat/indices"), eq(Map.of(
                "format", "json",
                "bytes", "b",
                "expand_wildcards", "all",
                "h", "index,health,status,pri,rep,docs.count,store.size")), isNull(), anyString());
    }

    @Test
    void withoutTheOpensearch3ModuleThereIsNoListAndOtherCallsAnswer503() {
        final IndexHealthService unavailable = new IndexHealthService(Optional.empty());

        assertThat(unavailable.catIndices()).isEmpty();
        assertThatThrownBy(unavailable::storeTypes).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> unavailable.clearCache("graylog_3")).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void readsStoreTypesOfIndicesThatSetOne() throws Exception {
        when(adapter.request(eq("GET"), eq("/_all/_settings/index.store.type"), anyMap(), isNull(), anyString()))
                .thenReturn(objectMapper.readTree("""
                        {"graylog_20":{"settings":{"index.store.type":"remote_snapshot"}},
                         "graylog_21":{"settings":{}},
                         "graylog_19":{"settings":{"index.store.type":null}}}"""));

        assertThat(service.storeTypes()).isEqualTo(Map.of("graylog_20", "remote_snapshot"));
    }

    @Test
    void clearsTheCacheOfExactlyOneIndex() {
        service.clearCache("graylog_3");

        verify(adapter).request(eq("POST"), eq("/graylog_3/_cache/clear"), eq(Map.of()), isNull(), any());
    }
}
