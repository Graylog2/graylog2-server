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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.plugin.database.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

@Singleton
public class EntityRegistrar {
    private static final Logger LOG = LoggerFactory.getLogger(EntityRegistrar.class);

    // TODO: get rid of this dependency
    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;
    private final Set<EntityRegistrationHandler> registrationHandlers;


    @Inject
    public EntityRegistrar(DBGrantService dbGrantService, GRNRegistry grnRegistry,
                           Set<EntityRegistrationHandler> registrationHandlers) {
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
        this.registrationHandlers = registrationHandlers;
    }

    public void registerNewEventDefinition(String id, User user) {
        registerNewEntity(id, user, GRNTypes.EVENT_DEFINITION);
    }

    public void registerNewEventNotification(String id, User user) {
        registerNewEntity(id, user, GRNTypes.EVENT_NOTIFICATION);
    }

    public void registerNewDashboard(String id, User user) {
        registerNewEntity(id, user, GRNTypes.DASHBOARD);
    }

    public void registerNewSearch(String id, User user) {
        registerNewEntity(id, user, GRNTypes.SEARCH);
    }

    public void registerNewStream(String id, User user) {
        registerNewEntity(id, user, GRNTypes.STREAM);
    }

    public void registerNewEntity(final String id, final User user, final GRNType grnType) {
        registerNewEntity(grnRegistry.newGRN(grnType, id), user);
    }

    public void registerNewEntity(GRN entityGRN, User user) {
        registrationHandlers.forEach(handler -> handler.handleRegistration(entityGRN, user));
    }

    public void unregisterEntity(final String id, final GRNType grnType) {
        GRN target = grnRegistry.newGRN(grnType, id);
        registrationHandlers.forEach(handler -> handler.handleUnregistration(target));
    }

    // TODO: move this method to a more appropriate place
    public List<GrantDTO> getGrantsForTarget(final GRNType type, final String id) {
        final GRN grn = grnRegistry.newGRN(type, id);
        return dbGrantService.getForTarget(grn);
    }

    public void unregisterStream(String id) {
        unregisterEntity(id, GRNTypes.STREAM);
    }

    public void unregisterDashboard(String id) {
        unregisterEntity(id, GRNTypes.DASHBOARD);
    }

    public void unregisterSearch(String id) {
        unregisterEntity(id, GRNTypes.SEARCH);
    }

    public void unregisterEventDefinition(String id) {
        unregisterEntity(id, GRNTypes.EVENT_DEFINITION);
    }

    public void unregisterEventNotification(String id) {
        unregisterEntity(id, GRNTypes.EVENT_NOTIFICATION);
    }
}
