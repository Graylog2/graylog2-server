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
import com.google.common.collect.Sets;
import org.graylog.plugins.sidecar.common.SidecarPluginConfiguration;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog2.migrations.Migration;
import org.graylog2.migrations.MigrationHelpers;
import org.graylog2.users.RoleService;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.UUID;

public class V20180323150000_AddSidecarUser extends Migration {
    private final RoleService roleService;
    private final String sidecarUser;
    private final MigrationHelpers helpers;

    @Inject
    public V20180323150000_AddSidecarUser(SidecarPluginConfiguration pluginConfiguration,
                                          RoleService roleService,
                                          MigrationHelpers migrationHelpers) {
        this.roleService = roleService;
        this.sidecarUser = pluginConfiguration.getUser();
        this.helpers = migrationHelpers;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-03-23T15:00:00Z");
    }

    @Override
    public void upgrade() {
        final String roleId = helpers.ensureBuiltinRole(
                "Sidecar System (Internal)",
                "Internal technical role. Grants access to register and pull configurations for a Sidecar node (built-in)",
                ImmutableSet.of(
                        SidecarRestPermissions.COLLECTORS_READ,
                        SidecarRestPermissions.CONFIGURATIONS_READ,
                        SidecarRestPermissions.SIDECARS_UPDATE));

        helpers.ensureUser(
                sidecarUser,
                UUID.randomUUID().toString(),
                "Sidecar", "System User (built-in)",
                "sidecar@graylog.local",
                Sets.newHashSet(
                        roleId,
                        roleService.getReaderRoleObjectId()));
    }
}
