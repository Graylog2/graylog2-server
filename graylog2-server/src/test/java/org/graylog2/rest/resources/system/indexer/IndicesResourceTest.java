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

import jakarta.ws.rs.ForbiddenException;
import org.assertj.core.api.Assertions;
import org.graylog2.indexer.NodeInfoCache;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.OutdatedIndex;
import org.graylog2.indexer.indices.OutdatedIndexService;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
class IndicesResourceTest {

    @Mock
    Indices indices;

    @Mock
    NodeInfoCache nodeInfoCache;

    @Mock
    IndexSetRegistry indexSetRegistry;

    @Mock
    OutdatedIndexService outdatedIndexService;

    @InjectMocks
    IndicesResource indicesResource;

    @Test
    @WithAuthorization(permissions = {"something:else"})
    void getOutdatedIndicesFailsIfNotPermitted() {
        Assertions.assertThatThrownBy(() -> indicesResource.getOutdatedIndices()).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @WithAuthorization(permissions = {"indices:read"})
    void getOutdatedIndicesSucceeds() {
        List<OutdatedIndex> outdatedIndices = List.of(
                new OutdatedIndex("outdated1", "1.3.0", false, false, null),
                new OutdatedIndex("outdated2", "1.3.0", true, true, "id1")
        );
        when(outdatedIndexService.getOutdatedIndices()).thenReturn(outdatedIndices);
        assertThat(indicesResource.getOutdatedIndices()).isEqualTo(outdatedIndices);
    }

    @Test
    @WithAuthorization(permissions = {"something:else"})
    void reindexFailsIfNotPermitted() {
        Assertions.assertThatThrownBy(() -> indicesResource.reindex("outdated", false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @WithAuthorization(permissions = {"indices:read", "indices:reindex"})
    void reindexSucceeds() {
        when(outdatedIndexService.getOutdatedIndices()).thenReturn(List.of(new OutdatedIndex(".outdated1", "1.3.0", false, false, null)));
        indicesResource.reindex(".outdated1", true);
        verify(outdatedIndexService, times(1)).reindex(".outdated1", true);
    }

}
