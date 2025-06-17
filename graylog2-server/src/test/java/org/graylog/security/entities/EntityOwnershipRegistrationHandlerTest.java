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
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityOwnershipRegistrationHandlerTest {

    private EntityOwnershipRegistrationHandler handler;
    private DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();
    private InOrder grnRegistryInOrderVerification;

    @BeforeEach
    void setUp() {
        this.dbGrantService = mock(DBGrantService.class);
        this.handler = new EntityOwnershipRegistrationHandler(dbGrantService, grnRegistry);
        this.grnRegistryInOrderVerification = Mockito.inOrder(dbGrantService);
    }

    @Test
    void registersNewEntityForEachType() {
        final User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("mockuser");
        when(mockUser.getId()).thenReturn("mockuser");
        final String id = "1234";


        for (GRNType type : GRNTypes.builtinTypes()) {
            handler.handleRegistration(grnRegistry.newGRN(type, id), mockUser);
            ArgumentCaptor<GrantDTO> grant = ArgumentCaptor.forClass(GrantDTO.class);
            ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
            grnRegistryInOrderVerification.verify(dbGrantService).create(grant.capture(), user.capture());

            assertThat(grant.getValue()).satisfies(g -> {
                assertThat(g.capability()).isEqualTo(Capability.OWN);
                assertThat(g.target().type()).isEqualTo(type.type());
                assertThat(g.target().entity()).isEqualTo(id);
                assertThat(g.grantee().type()).isEqualTo(GRNTypes.USER.type());
                assertThat(g.grantee().entity()).isEqualTo("mockuser");
            });
        }
    }

    @Test
    void unregistersEntityForEachType() {
        for (GRNType type : GRNTypes.builtinTypes()) {
            handler.handleUnregistration(grnRegistry.newGRN(type, "1234"));
            assertGrantRemoval(type, "1234");
        }
    }

    private void assertGrantRemoval(GRNType grnType, String entity) {
        ArgumentCaptor<GRN> argCaptor = ArgumentCaptor.forClass(GRN.class);
        grnRegistryInOrderVerification.verify(dbGrantService).deleteForTarget(argCaptor.capture());

        assertThat(argCaptor.getValue()).satisfies(grn -> {
            assertThat(grn.grnType()).isEqualTo(grnType);
            assertThat(grn.entity()).isEqualTo(entity);
        });
    }
}
