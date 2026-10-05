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
package org.graylog2.rest.resources.system;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
class DeflectorResourceTest {

    @Mock
    private IndexSetRegistry indexSetRegistry;

    @Mock
    private ActivityWriter activityWriter;

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private UserContext userContext;

    private DeflectorResource deflectorResource;

    @BeforeEach
    void setUp() {
        deflectorResource = new DeflectorResource(indexSetRegistry, activityWriter, auditEventSender,
                new ObjectMapperProvider().get());
    }

    private static IndexSet writableIndexSet() {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.isWritable()).thenReturn(true);

        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getConfig()).thenReturn(config);
        return indexSet;
    }

    private static IndexSet nonWritableIndexSet(String id, String title) {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.isWritable()).thenReturn(false);
        when(config.id()).thenReturn(id);
        when(config.title()).thenReturn(title);

        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getConfig()).thenReturn(config);
        return indexSet;
    }

    @Test
    @WithAuthorization(permissions = {"deflector:cycle"})
    void bulkCycleCyclesWritableIndexSetsAndReportsFailures() {
        final IndexSet writable = writableIndexSet();
        final IndexSet nonWritable = nonWritableIndexSet("nonwritable1", "Non-writable 1");

        when(indexSetRegistry.get("writable1")).thenReturn(Optional.of(writable));
        when(indexSetRegistry.get("nonwritable1")).thenReturn(Optional.of(nonWritable));
        when(indexSetRegistry.get("missing")).thenReturn(Optional.empty());

        final Response response = deflectorResource.bulkCycle(
                new BulkOperationRequest(List.of("writable1", "nonwritable1", "missing")), userContext);

        assertThat(response.getStatus()).isEqualTo(200);
        final BulkOperationResponse body = (BulkOperationResponse) response.getEntity();

        verify(writable).cycle();
        verify(nonWritable, never()).cycle();

        assertThat(body.successfullyPerformed()).isEqualTo(1);
        assertThat(body.failures()).extracting(BulkOperationFailure::entityId)
                .containsExactlyInAnyOrder("nonwritable1", "missing");
    }

    @Test
    @WithAuthorization(permissions = {"deflector:cycle"})
    void bulkCycleRejectsEmptyRequest() {
        assertThatThrownBy(() -> deflectorResource.bulkCycle(new BulkOperationRequest(List.of()), userContext))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @WithAuthorization(permissions = {"something:else"})
    void bulkCycleReportsFailureWhenNotPermitted() {
        final Response response = deflectorResource.bulkCycle(
                new BulkOperationRequest(List.of("writable1")), userContext);

        final BulkOperationResponse body = (BulkOperationResponse) response.getEntity();

        assertThat(body.successfullyPerformed()).isEqualTo(0);
        assertThat(body.failures()).extracting(BulkOperationFailure::entityId).containsExactly("writable1");
        verifyNoInteractions(indexSetRegistry);
    }
}
