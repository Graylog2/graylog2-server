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
package org.graylog.collectors.rest;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.security.Permission.create;

public class FleetPermissions implements PluginPermissions {
    public static final String FLEET_CREATE = "collectors_fleets:create";
    public static final String FLEET_READ = "collectors_fleets:read";
    public static final String FLEET_EDIT = "collectors_fleets:edit";
    public static final String FLEET_DELETE = "collectors_fleets:delete";

    public static final String SOURCE_CREATE = "collectors_sources:create";
    public static final String SOURCE_READ = "collectors_sources:read";
    public static final String SOURCE_EDIT = "collectors_sources:edit";
    public static final String SOURCE_DELETE = "collectors_sources:delete";

    private static final ImmutableSet<Permission> PERMISSIONS = ImmutableSet.of(
            create(FLEET_CREATE, "Create a new fleet"),
            create(FLEET_READ, "Read fleet details"),
            create(FLEET_EDIT, "Edit fleet details"),
            create(FLEET_DELETE, "Delete a fleet"),
            create(SOURCE_CREATE, "Create a new source in a fleet"),
            create(SOURCE_READ, "Read a source details in a fleet"),
            create(SOURCE_EDIT, "Edit a source details in a fleet"),
            create(SOURCE_DELETE, "Delete a source from a fleet")
    );

    @Override
    public Set<Permission> permissions() {
        return PERMISSIONS;
    }

    @Override
    public Set<Permission> readerBasePermissions() {
        return Set.of();
    }
}
