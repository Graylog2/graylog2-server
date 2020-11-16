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
package org.graylog.plugins.sidecar.permissions;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Collections;
import java.util.Set;

import static org.graylog2.plugin.security.Permission.create;

public class SidecarRestPermissions implements PluginPermissions {
    public static final String SIDECARS_READ = "sidecars:read";
    public static final String SIDECARS_CREATE = "sidecars:create";
    public static final String SIDECARS_UPDATE = "sidecars:update";
    public static final String SIDECARS_DELETE = "sidecars:delete";

    public static final String COLLECTORS_READ = "sidecar_collectors:read";
    public static final String COLLECTORS_CREATE = "sidecar_collectors:create";
    public static final String COLLECTORS_UPDATE = "sidecar_collectors:update";
    public static final String COLLECTORS_DELETE = "sidecar_collectors:delete";

    public static final String CONFIGURATIONS_READ = "sidecar_collector_configurations:read";
    public static final String CONFIGURATIONS_CREATE = "sidecar_collector_configurations:create";
    public static final String CONFIGURATIONS_UPDATE = "sidecar_collector_configurations:update";
    public static final String CONFIGURATIONS_DELETE = "sidecar_collector_configurations:delete";

    private final ImmutableSet<Permission> permissions = ImmutableSet.of(
            create(SIDECARS_READ, "Read sidecars"),
            create(SIDECARS_CREATE, "Create sidecars"),
            create(SIDECARS_UPDATE, "Update sidecars"),
            create(SIDECARS_DELETE, "Delete sidecars"),

            create(COLLECTORS_READ, "Read collectors"),
            create(COLLECTORS_CREATE, "Create collectors"),
            create(COLLECTORS_UPDATE, "Update collectors"),
            create(COLLECTORS_DELETE, "Delete collectors"),

            create(CONFIGURATIONS_READ, "Read configurations"),
            create(CONFIGURATIONS_CREATE, "Create configurations"),
            create(CONFIGURATIONS_UPDATE, "Update configurations"),
            create(CONFIGURATIONS_DELETE, "Delete configurations")
    );

    @Override
    public Set<Permission> permissions() {
        return permissions;
    }

    @Override
    public Set<Permission> readerBasePermissions() {
        return Collections.emptySet();
    }
}
