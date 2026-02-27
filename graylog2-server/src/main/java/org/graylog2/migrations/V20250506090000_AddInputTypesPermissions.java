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
import org.apache.commons.collections4.CollectionUtils;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

public class V20250506090000_AddInputTypesPermissions extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20250506090000_AddInputTypesPermissions.class);

    private final ClusterConfigService clusterConfigService;
    private final RoleService roleService;
    private final UserService userService;

    @Inject
    public V20250506090000_AddInputTypesPermissions(final ClusterConfigService clusterConfigService,
                                                    RoleService roleService, UserService userService) {
        this.clusterConfigService = clusterConfigService;
        this.roleService = roleService;
        this.userService = userService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-05-06T09:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20250506090000_AddInputTypesPermissions.MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        LOG.debug("Starting migration to add input types permissions.");
        roleService.loadAll().stream()
                .filter(role -> !role.isReadOnly())
                .filter(role -> CollectionUtils.containsAny(role.getPermissions(),
                        RestPermissions.INPUTS_CHANGESTATE, RestPermissions.INPUTS_CREATE,
                        RestPermissions.INPUTS_EDIT, RestPermissions.INPUTS_TERMINATE))
                .peek(role -> {
                    Set<String> permissions = role.getPermissions();
                    permissions.add(RestPermissions.INPUT_TYPES_CREATE);
                    role.setPermissions(permissions);
                }).forEach(role -> {
                    LOG.info("Updating role {} to include input type permission", role.getName());
                    try {
                        roleService.save(role);
                    } catch (ValidationException e) {
                        LOG.error("Error updating role.", e);
                    }
                });

        userService.loadAll().stream()
                .filter(user -> CollectionUtils.containsAny(user.getPermissions(),
                        RestPermissions.INPUTS_CHANGESTATE, RestPermissions.INPUTS_CREATE,
                        RestPermissions.INPUTS_EDIT, RestPermissions.INPUTS_TERMINATE))
                .peek(user -> {
                    List<String> permissions = user.getPermissions();
                    permissions.add(RestPermissions.INPUT_TYPES_CREATE);
                    user.setPermissions(permissions);
                }).forEach(user -> {
                    LOG.info("Updating user {} to include individual input type permission", user.getName());
                    try {
                        userService.save(user);
                    } catch (ValidationException e) {
                        LOG.error("Error updating user.", e);
                    }
                });

        clusterConfigService.write(new V20250506090000_AddInputTypesPermissions.MigrationCompleted());
    }

    public record MigrationCompleted() {
    }
}
