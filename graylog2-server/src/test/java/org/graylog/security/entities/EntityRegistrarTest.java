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

import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.DBGrantService;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.mockito.Mockito.mock;

class EntityRegistrarTest {

    private final GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    private EntityRegistrar entityRegistrar;
    private DBGrantService dbGrantService;

    private EntityRegistrationHandler handler1;
    private EntityRegistrationHandler handler2;
    private Set<EntityRegistrationHandler> registrationHandlers;

    @BeforeEach
    void setUp() {
        this.handler1 = mock(EntityRegistrationHandler.class);
        this.handler2 = mock(EntityRegistrationHandler.class);
        this.registrationHandlers = Set.of(handler1, handler2);

        this.dbGrantService = mock(DBGrantService.class);
        this.entityRegistrar = new EntityRegistrar(dbGrantService, grnRegistry, () -> registrationHandlers);
    }

    @Test
    void registerNewEntityByIdAndType() {
        final var user = mock(User.class);
        entityRegistrar.registerNewEntity("1234", user, GRNTypes.DASHBOARD);
        Mockito.verify(handler1).handleRegistration(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1234"), user);
        Mockito.verify(handler2).handleRegistration(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1234"), user);
    }

    @Test
    void registerNewEntityByGRN() {
        final var user = mock(User.class);
        entityRegistrar.registerNewEntity(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1234"), user);
        Mockito.verify(handler1).handleRegistration(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1234"), user);
        Mockito.verify(handler2).handleRegistration(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1234"), user);
    }

    @Test
    void unregisterEntity() {
        entityRegistrar.unregisterEntity("1234", GRNTypes.DASHBOARD);
        Mockito.verify(handler1).handleUnregistration(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1234"));
        Mockito.verify(handler2).handleUnregistration(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1234"));
    }
}
