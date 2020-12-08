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
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.plugin.database.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EntityOwnershipService {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipService.class);

    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;


    @Inject
    public EntityOwnershipService(DBGrantService dbGrantService,
                                  GRNRegistry grnRegistry) {
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
    }

    public void registerNewEventDefinition(String id, User user) {
        final GRN grn = grnRegistry.newGRN(GRNTypes.EVENT_DEFINITION, id);
        registerNewEntity(grn, user);
    }

    public void registerNewEventNotification(String id, User user) {
        final GRN grn = grnRegistry.newGRN(GRNTypes.EVENT_NOTIFICATION, id);
        registerNewEntity(grn, user);
    }

    public void registerNewDashboard(String id, User user) {
        final GRN grn = grnRegistry.newGRN(GRNTypes.DASHBOARD, id);
        registerNewEntity(grn, user);
    }

    public void registerNewSearch(String id, User user) {
        final GRN grn = grnRegistry.newGRN(GRNTypes.SEARCH, id);
        registerNewEntity(grn, user);
    }

    public void registerNewStream(String id, User user) {
        final GRN grn = grnRegistry.newGRN(GRNTypes.STREAM, id);
        registerNewEntity(grn, user);
    }

    private void registerNewEntity(GRN entity, User user) {
        // Don't create ownership grants for the admin user.
        // They can access anything anyhow
        if (user.isLocalAdmin()) {
            return;
        }

        dbGrantService.create(GrantDTO.builder()
                .capability(Capability.OWN)
                .target(entity)
                .grantee(grnRegistry.ofUser(user))
                .build(), user);
    }

    public void unregisterStream(String id) {
        removeGrantsForTarget(grnRegistry.newGRN(GRNTypes.STREAM, id));
    }

    public void unregisterDashboard(String id) {
        removeGrantsForTarget(grnRegistry.newGRN(GRNTypes.DASHBOARD, id));
    }

    public void unregisterSearch(String id) {
        removeGrantsForTarget(grnRegistry.newGRN(GRNTypes.SEARCH, id));
    }

    public void unregisterEventDefinition(String id) {
        removeGrantsForTarget(grnRegistry.newGRN(GRNTypes.EVENT_DEFINITION, id));
    }

    public void unregisterEventNotification(String id) {
        removeGrantsForTarget(grnRegistry.newGRN(GRNTypes.EVENT_NOTIFICATION, id));
    }

    private void removeGrantsForTarget(GRN target) {
        LOG.debug("Removing grants for <{}>", target);
        dbGrantService.deleteForTarget(target);
    }
}
