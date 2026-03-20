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
package org.graylog.collectors;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Set;

import static org.graylog2.plugin.security.Permission.create;

public class CollectorsPermissions implements PluginPermissions {
    public static final String FLEET_CREATE = "collectors_fleets:create";
    public static final String FLEET_READ = "collectors_fleets:read";
    public static final String FLEET_EDIT = "collectors_fleets:edit";
    public static final String FLEET_DELETE = "collectors_fleets:delete";
    public static final String FLEET_INSTANCE_ASSIGN = "collectors_fleets:assign_instance";
    public static final String FLEET_INSTANCE_DELETE = "collectors_fleets:delete_instance";

    // these are scoped to fleets, not individual sources!
    public static final String SOURCE_CREATE = "collectors_fleets:source_create";
    public static final String SOURCE_EDIT = "collectors_fleets:source_edit";
    public static final String SOURCE_DELETE = "collectors_fleets:source_delete";

    public static final String CONFIGURATION_READ = "collectors_config:read";
    public static final String CONFIGURATION_EDIT = "collectors_config:edit";

    public static final String ACTIVITY_READ = "collectors_activity:read";

    private static final ImmutableSet<Permission> PERMISSIONS = ImmutableSet.of(
            create(FLEET_CREATE, "Create a new fleet"),
            create(FLEET_READ, "Read fleet details"),
            create(FLEET_EDIT, "Edit fleet details"),
            create(FLEET_DELETE, "Delete a fleet"),
            create(FLEET_INSTANCE_ASSIGN, "Assign a collector to a fleet"),
            create(FLEET_INSTANCE_DELETE, "Delete a collector instance"),
            create(SOURCE_CREATE, "Create a new source in a fleet"),
            create(SOURCE_EDIT, "Edit source details in a fleet"),
            create(SOURCE_DELETE, "Delete a source in a fleet"),
            create(CONFIGURATION_READ, "Read the configuration for collectors"),
            create(CONFIGURATION_EDIT, "Edit the configuration for collectors"),
            create(ACTIVITY_READ, "Read the recent activity feed")
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
