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
package org.graylog2.migrations;

import jakarta.inject.Inject;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Set;

public class V20250721090000_AddClusterConfigurationPermission extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20250721090000_AddClusterConfigurationPermission.class);

    static final String CLUSTER_CONFIGURATION_READER_ROLE = "Cluster Configuration Reader";

    private final ClusterConfigService clusterConfigService;
    private final RoleService roleService;
    private final UserService userService;

    @Inject
    public V20250721090000_AddClusterConfigurationPermission(final ClusterConfigService clusterConfigService,
                                                             RoleService roleService, UserService userService) {
        this.clusterConfigService = clusterConfigService;
        this.roleService = roleService;
        this.userService = userService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-07-21T09:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20250721090000_AddClusterConfigurationPermission.MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        LOG.debug("Starting migration to add cluster configuration reader role to users with reader role.");

        try {
            Role readerRole = roleService.loadById(roleService.getReaderRoleObjectId());
            Role clusterConfigurationReaderRole = roleService.load(CLUSTER_CONFIGURATION_READER_ROLE);

            userService.loadAllForRole(readerRole).stream()
                    .peek(user -> {
                        Set<String> roleIds = user.getRoleIds();
                        roleIds.add(clusterConfigurationReaderRole.getId());
                        user.setRoleIds(roleIds);
                    }).forEach(user -> {
                        LOG.debug("Updating user {} with new cluster configuration reader role", user.getName());
                        try {
                            userService.save(user);
                        } catch (ValidationException e) {
                            LOG.error("Error updating user.", e);
                        }
                    });


        } catch (NotFoundException e) {
            LOG.error("Built-in role not found. Cannot add cluster configuration role to users with reader role: {}", e.getMessage());
        } finally {
            clusterConfigService.write(new V20250721090000_AddClusterConfigurationPermission.MigrationCompleted());
        }

    }

    public record MigrationCompleted() {
    }
}
