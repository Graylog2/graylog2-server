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
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.plugin.database.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class EntityOwnershipRegistrationHandler implements EntityRegistrationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipRegistrationHandler.class);

    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;


    @Inject
    public EntityOwnershipRegistrationHandler(DBGrantService dbGrantService,
                                              GRNRegistry grnRegistry) {
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
    }

    @Override
    public void handleUnregistration(GRN entityGRN) {
        LOG.debug("Removing grants for <{}>", entityGRN);
        dbGrantService.deleteForTarget(entityGRN);
    }

    @Override
    public void handleRegistration(GRN entityGRN, User user) {
        // Don't create ownership grants for the admin user.
        // They can access anything anyhow
        if (user.isLocalAdmin()) {
            return;
        }

        final var grantee = grnRegistry.ofUser(user);

        dbGrantService.create(GrantDTO.builder()
                .capability(Capability.OWN)
                .target(entityGRN)
                .grantee(grantee)
                .build(), user);
    }

}
