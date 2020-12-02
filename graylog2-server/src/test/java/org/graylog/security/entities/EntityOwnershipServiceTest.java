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
package org.graylog.security.entities;

import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntityOwnershipServiceTest {

    private EntityOwnershipService entityOwnershipService;
    private DBGrantService dbGrantService;
    private GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    @BeforeEach
    void setUp() {
        this.dbGrantService = mock(DBGrantService.class);
        this.entityOwnershipService = new EntityOwnershipService(dbGrantService, grnRegistry);
    }

    @Test
    void registerNewEventDefinition() {
        final User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("mockuser");
        when(mockUser.getId()).thenReturn("mockuser");

        entityOwnershipService.registerNewEventDefinition("1234", mockUser);

        ArgumentCaptor<GrantDTO> grant = ArgumentCaptor.forClass(GrantDTO.class);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(dbGrantService).create(grant.capture(), user.capture());

        assertThat(grant.getValue()).satisfies(g -> {
            assertThat(g.capability()).isEqualTo(Capability.OWN);
            assertThat(g.target().type()).isEqualTo(GRNTypes.EVENT_DEFINITION.type());
            assertThat(g.target().entity()).isEqualTo("1234");
            assertThat(g.grantee().type()).isEqualTo(GRNTypes.USER.type());
            assertThat(g.grantee().entity()).isEqualTo("mockuser");
        });
    }

    @Test
    void unregisterDashboard() {
        entityOwnershipService.unregisterDashboard("1234");
        assertGrantRemoval(GRNTypes.DASHBOARD, "1234");
    }

    @Test
    void unregisterSearch() {
        entityOwnershipService.unregisterSearch("1234");
        assertGrantRemoval(GRNTypes.SEARCH, "1234");
    }

    @Test
    void unregisterEventDefinition() {
        entityOwnershipService.unregisterEventDefinition("1234");
        assertGrantRemoval(GRNTypes.EVENT_DEFINITION, "1234");
    }

    @Test
    void unregisterEventNotification() {
        entityOwnershipService.unregisterEventNotification("1234");
        assertGrantRemoval(GRNTypes.EVENT_NOTIFICATION, "1234");
    }

    @Test
    void unregisterStream() {
        entityOwnershipService.unregisterStream("123");
        assertGrantRemoval(GRNTypes.STREAM, "123");
    }

    private void assertGrantRemoval(GRNType grnType, String entity) {
        ArgumentCaptor<GRN> argCaptor = ArgumentCaptor.forClass(GRN.class);
        verify(dbGrantService).deleteForTarget(argCaptor.capture());

        assertThat(argCaptor.getValue()).satisfies(grn -> {
            assertThat(grn.grnType()).isEqualTo(grnType);
            assertThat(grn.entity()).isEqualTo(entity);
        });
    }
}
