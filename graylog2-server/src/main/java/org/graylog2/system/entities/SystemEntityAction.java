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
package org.graylog2.system.entities;

/**
 * An action that can be performed on a {@link SystemEntity}.  An action can be allowed or denied irrespective of user roles or permissions.
 *
 * <p>
 * For example, a user may have the permission <b>anomaly_configuration:edit</b> which allows her/him to edit Anomaly Detectors.  However, we may not want users to edit Anomaly Detectors installed via content packs as these could be updated or removed in future versions.  The user could clone such an entity, which has no action restrictions, and is not controlled/manage by Graylog and thus has no danger of being modified in anyway by a future version.
 * </p>
 */
public enum SystemEntityAction {
    EDIT,
    DELETE,
    VIEW,
    LIST,
    EXPORT
}
