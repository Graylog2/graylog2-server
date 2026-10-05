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
package org.graylog2.rest.resources.system.indexer;

import jakarta.ws.rs.ServiceUnavailableException;
import org.apache.shiro.subject.Subject;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.IndexHealthService;
import org.graylog2.indexer.management.IndexOverview;
import org.graylog2.indexer.management.TestSubjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexManagementResourceTest {
    private final IndexHealthService indexHealthService = mock(IndexHealthService.class);
    private final IndexSetRegistry indexSetRegistry = mock(IndexSetRegistry.class);

    private Subject subject = TestSubjects.admin();
    private IndexManagementResource resource;

    @BeforeEach
    void setUp() {
        resource = new IndexManagementResource(indexHealthService, indexSetRegistry) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
        when(indexHealthService.storeTypes()).thenReturn(Map.of());
    }

    @Test
    void addsIndexSetWriteIndexAndTierToOpenSearchsView() throws Exception {
        final IndexSet defaultSet = indexSet("set-1", "Default index set", "graylog_21");
        when(indexHealthService.catIndices()).thenReturn(Optional.of(List.of(
                new CatIndex("graylog_21", "green", "open", 1, 0, 5L, 900L),
                new CatIndex("graylog_20", "red", "open", 1, 0, null, null),
                new CatIndex("security-auditlog", "yellow", "open", 1, 1, 7L, 1200L))));
        when(indexSetRegistry.getForIndex("graylog_21")).thenReturn(Optional.of(defaultSet));
        when(indexSetRegistry.getForIndex("graylog_20")).thenReturn(Optional.of(defaultSet));
        when(indexHealthService.storeTypes()).thenReturn(Map.of("graylog_20", "remote_snapshot", "graylog_21", "fs"));

        final List<IndexOverview> indices = resource.list().indices();

        // Sorted by name.
        assertThat(indices).extracting(IndexOverview::index).containsExactly("graylog_20", "graylog_21", "security-auditlog");
        assertThat(indices.get(0)).isEqualTo(new IndexOverview("graylog_20", "red", "open", 1, 0, null, null,
                "set-1", "Default index set", false, IndexOverview.TIER_WARM));
        assertThat(indices.get(1)).isEqualTo(new IndexOverview("graylog_21", "green", "open", 1, 0, 5L, 900L,
                "set-1", "Default index set", true, IndexOverview.TIER_HOT));
        assertThat(indices.get(2)).isEqualTo(new IndexOverview("security-auditlog", "yellow", "open", 1, 1, 7L, 1200L,
                null, null, false, IndexOverview.TIER_HOT));
        // The write index is looked up once per index set, not once per index.
        verify(defaultSet, times(1)).getActiveWriteIndex();
    }

    @Test
    void listsOnlyIndicesTheUserMayRead() {
        subject = TestSubjects.withPermissions("indices:read:graylog_3");
        when(indexHealthService.catIndices()).thenReturn(Optional.of(List.of(
                new CatIndex("graylog_3", "green", "open", 1, 0, 0L, 208L),
                new CatIndex("graylog_4", "green", "open", 1, 0, 0L, 208L))));

        assertThat(resource.list().indices()).extracting(IndexOverview::index).containsExactly("graylog_3");
    }

    @Test
    void anUnresolvableWriteIndexMarksNoIndexAsWriteIndex() throws Exception {
        final IndexSet broken = indexSet("set-1", "Default index set", null);
        when(broken.getActiveWriteIndex()).thenThrow(new IllegalStateException("alias missing"));
        when(indexHealthService.catIndices()).thenReturn(Optional.of(List.of(
                new CatIndex("graylog_20", "red", "open", 1, 0, null, null))));
        when(indexSetRegistry.getForIndex("graylog_20")).thenReturn(Optional.of(broken));

        assertThat(resource.list().indices()).singleElement()
                .satisfies(index -> assertThat(index.isWriteIndex()).isFalse());
    }

    @Test
    void withoutTheOpensearch3ModuleTheListAnswers503() {
        when(indexHealthService.catIndices()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.list()).isInstanceOf(ServiceUnavailableException.class);
    }

    private static IndexSet indexSet(String id, String title, String activeWriteIndex) throws Exception {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.id()).thenReturn(id);
        when(config.title()).thenReturn(title);
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getConfig()).thenReturn(config);
        when(indexSet.getActiveWriteIndex()).thenReturn(activeWriteIndex);
        return indexSet;
    }
}
