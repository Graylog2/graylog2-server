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
import org.graylog.security.authzroles.BuiltinRole;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Set;

import static org.graylog2.plugin.security.Permission.create;

public class CollectorsPermissions implements PluginPermissions {
    public static final String FLEET_CREATE = "collector_fleets:create";
    public static final String FLEET_READ = "collector_fleets:read";
    public static final String FLEET_EDIT = "collector_fleets:edit";
    public static final String FLEET_DELETE = "collector_fleets:delete";
    public static final String FLEET_INSTANCE_ASSIGN = "collector_fleets:assign_instance";
    public static final String FLEET_INSTANCE_DELETE = "collector_fleets:delete_instance";

    // these are scoped to fleets, not individual sources!
    public static final String SOURCE_CREATE = "collector_fleets:source_create";
    public static final String SOURCE_EDIT = "collector_fleets:source_edit";
    public static final String SOURCE_DELETE = "collector_fleets:source_delete";

    public static final String CONFIGURATION_READ = "collectors_config:read";
    public static final String CONFIGURATION_EDIT = "collectors_config:edit";

    public static final String ACTIVITIES_READ = "collector_activities:read";

    public static final String TOKEN_CREATE = "collector_enrollment_tokens:create";
    public static final String TOKEN_READ = "collector_enrollment_tokens:read";
    public static final String TOKEN_DELETE = "collector_enrollment_tokens:delete";

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
            create(ACTIVITIES_READ, "Read the recent activity feed"),
            create(TOKEN_CREATE, "Create an enrollment token for a fleet"),
            create(TOKEN_READ, "Read enrollment tokens for a fleet"),
            create(TOKEN_DELETE, "Delete an enrollment token for a fleet")
    );

    @Override
    public Set<Permission> permissions() {
        return PERMISSIONS;
    }

    @Override
    public Set<Permission> readerBasePermissions() {
        return Set.of();
    }

    @Override
    public Set<BuiltinRole> builtinRoles() {
        return Set.of(
                BuiltinRole.create("Collectors Manager",
                        "Grants full control of collectors and fleets (built-in)",
                        Set.of(FLEET_CREATE, FLEET_READ, FLEET_EDIT, FLEET_DELETE,
                                FLEET_INSTANCE_ASSIGN, FLEET_INSTANCE_DELETE,
                                SOURCE_CREATE, SOURCE_EDIT, SOURCE_DELETE,
                                TOKEN_CREATE, TOKEN_READ, TOKEN_DELETE,
                                CONFIGURATION_READ, CONFIGURATION_EDIT,
                                ACTIVITIES_READ)),
                BuiltinRole.create("Collectors Reader",
                        "Grants read-only access to collectors and fleets (built-in)",
                        Set.of(FLEET_READ, CONFIGURATION_READ, ACTIVITIES_READ)));
    }
}
