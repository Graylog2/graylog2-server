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
package org.graylog.plugins.sidecar.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog2.migrations.Migration;
import org.graylog2.migrations.MigrationHelpers;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;

public class V20230502164900_AddSidecarManagerAndReaderRole extends Migration {

    private final MigrationHelpers helpers;

    @Inject
    public V20230502164900_AddSidecarManagerAndReaderRole(MigrationHelpers migrationHelpers) {
        this.helpers = migrationHelpers;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-02T16:49:00Z");
    }

    @Override
    public void upgrade() {
        helpers.ensureBuiltinRole(
                "Sidecar Manager",
                "Grants access to read, register and pull configurations for Sidecars (built-in)",
                ImmutableSet.of(
                        SidecarRestPermissions.COLLECTORS_READ,
                        SidecarRestPermissions.COLLECTORS_CREATE,
                        SidecarRestPermissions.COLLECTORS_UPDATE,
                        SidecarRestPermissions.COLLECTORS_DELETE,
                        SidecarRestPermissions.CONFIGURATIONS_READ,
                        SidecarRestPermissions.CONFIGURATIONS_CREATE,
                        SidecarRestPermissions.CONFIGURATIONS_UPDATE,
                        SidecarRestPermissions.CONFIGURATIONS_DELETE,
                        SidecarRestPermissions.SIDECARS_READ,
                        SidecarRestPermissions.SIDECARS_CREATE,
                        SidecarRestPermissions.SIDECARS_UPDATE,
                        SidecarRestPermissions.SIDECARS_DELETE));
        helpers.ensureBuiltinRole(
                "Sidecar Reader",
                "Grants access to read configurations for Sidecars (built-in)",
                ImmutableSet.of(
                        SidecarRestPermissions.COLLECTORS_READ,
                        SidecarRestPermissions.CONFIGURATIONS_READ,
                        SidecarRestPermissions.SIDECARS_READ));

    }
}
