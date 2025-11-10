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
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.plugin.database.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Handles the registration and unregistration of entity ownership grants.
 *
 * When a new entity is registered, this handler assigns an ownership grant to the user
 * (unless the user is a local admin or the entity is a user entity).
 * When an entity is unregistered, it removes all grants for that entity.
 */
@Singleton
public class EntityOwnershipRegistrationHandler implements EntityRegistrationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipRegistrationHandler.class);

    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;

    /**
     * Constructs a new EntityOwnershipRegistrationHandler.
     *
     * @param dbGrantService the service for managing database grants
     * @param grnRegistry    the registry for GRN (Graylog Resource Name) objects
     */
    @Inject
    public EntityOwnershipRegistrationHandler(DBGrantService dbGrantService,
                                              GRNRegistry grnRegistry) {
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
    }

    /**
     * Handles the unregistration of an entity by removing all grants for the given entity GRN.
     *
     * @param entityGRN the GRN of the entity being unregistered
     */
    @Override
    public void handleUnregistration(GRN entityGRN) {
        LOG.debug("Removing grants for <{}>", entityGRN);
        dbGrantService.deleteForTarget(entityGRN);
    }

    /**
     * Handles the registration of a new entity by assigning an ownership grant to the user.
     * Skips grant creation for local admin users and user entities.
     *
     * @param entityGRN the GRN of the entity being registered
     * @param user      the user associated with the registration
     */
    @Override
    public void handleRegistration(GRN entityGRN, User user) {
        // Don't create ownership grants for the admin user.
        // They can access anything anyhow
        if (user.isLocalAdmin()) {
            return;
        }

        // Don't create ownership grants for user entities. It wouldn't technically be wrong, but we have no use for it
        if (GRNTypes.USER.equals(entityGRN.grnType())) {
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
