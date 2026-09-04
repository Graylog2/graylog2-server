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
import org.graylog2.plugin.database.users.User;

/**
 * Handler interface for entity registration and unregistration events.
 * Implementations can perform additional actions when entities are registered or unregistered.
 */
public interface EntityRegistrationHandler {
    /**
     * Handles the registration of a new entity.
     *
     * @param entityGRN the GRN of the entity being registered
     * @param user      the user associated with the registration
     */
    void handleRegistration(GRN entityGRN, User user);

    /**
     * Handles the unregistration of an entity.
     *
     * @param entityGRN the GRN of the entity being unregistered
     */
    void handleUnregistration(GRN entityGRN);
}
