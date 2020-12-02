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
package org.graylog2.migrations.V20200803120800_GrantsMigrations;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog2.migrations.V20200803120800_GrantsMigrations.GrantsMetaMigration.GRNTypeCapability;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog.plugins.views.search.rest.ViewsRestPermissions.VIEW_READ;

public class UserPermissionsToGrantsMigration {
    private static final Logger LOG = LoggerFactory.getLogger(UserPermissionsToGrantsMigration.class);
    private final UserService userService;
    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;
    private final ViewService viewService;
    private final String rootUsername;

    public UserPermissionsToGrantsMigration(UserService userService,
                                            DBGrantService dbGrantService,
                                            GRNRegistry grnRegistry,
                                            ViewService viewService,
                                            @Named("root_username") String rootUsername) {
        this.userService = userService;
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
        this.viewService = viewService;
        this.rootUsername = rootUsername;
    }

    public void upgrade() {
        final List<User> users = userService.loadAll();

        for (User user : users) {
            final Map<String, Set<String>> migratableEntities = getMigratableEntities(ImmutableSet.copyOf(user.getPermissions()));
            if (!migratableEntities.isEmpty()) {
                migrateUserPermissions(user, migratableEntities);
            }
        }
    }

    private Optional<GRNType> getViewGRNType(String viewId) {
        return viewService.get(viewId).map(view -> {
            final GRNType viewGrnType;
            switch (view.type()) {
                case SEARCH:
                    viewGrnType = GRNTypes.SEARCH;
                    break;
                case DASHBOARD:
                    viewGrnType = GRNTypes.DASHBOARD;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + view.type());
            }
            return viewGrnType;
        });
    }

    private void migrateUserPermissions(User user, Map<String, Set<String>> migratableEntities) {
        migratableEntities.forEach((entityID, permissions) -> {
            final GRNTypeCapability grnTypeCapability = GrantsMetaMigration.MIGRATION_MAP.get(permissions);

            // Permissions are mappable to a grant
            if (grnTypeCapability != null) {
                final Capability capability = grnTypeCapability.capability;

                GRN targetGRN;
                if (permissions.stream().anyMatch(p -> p.contains(VIEW_READ))) {
                    // For views we need to load the database object to be able to determine if it's a
                    // search or a dashboard.
                    targetGRN = getViewGRNType(entityID).map(grnType -> grnType.toGRN(entityID)).orElse(null);
                } else {
                    targetGRN = requireNonNull(grnTypeCapability.grnType, "grnType cannot be null - this is a bug").toGRN(entityID);
                }

                if (targetGRN != null) {
                    dbGrantService.ensure(grnRegistry.ofUser(user), capability, targetGRN, rootUsername);
                }

                final List<String> updatedPermissions = user.getPermissions();
                updatedPermissions.removeAll(permissions.stream().map(p -> p + ":" + entityID).collect(Collectors.toSet()));
                user.setPermissions(updatedPermissions);
                try {
                    userService.save(user);
                } catch (ValidationException e) {
                    LOG.error("Failed to update permssions on user <{}>", user.getName(), e);
                }
                LOG.info("Migrating entity <{}> permissions <{}> to <{}> grant for user <{}>", targetGRN, permissions, capability, user.getName());
            } else {
                LOG.info("Skipping non-migratable entity <{}>. Permissions <{}> cannot be converted to a grant capability", entityID, permissions);
            }
        });
    }

    private Map<String, Set<String>> getMigratableEntities(Set<String> permissions) {
        Map<String, Set<String>> migratableEntities = new HashMap<>();

        permissions.stream().map(GrantsMetaMigration.MigrationWildcardPermission::new)
                .filter(p -> p.getParts().size() == 3 && p.getParts().stream().allMatch(part -> part.size() == 1))
                .forEach(p -> {
                    String permissionType = p.subPart(0);
                    String restPermission = p.subPart(0) + ":" + p.subPart(1);
                    String id = p.subPart(2);

                    if (GrantsMetaMigration.MIGRATION_MAP.keySet().stream().flatMap(Collection::stream).anyMatch(perm -> perm.startsWith(permissionType + ":"))) {
                        if (migratableEntities.containsKey(id)) {
                            migratableEntities.get(id).add(restPermission);
                        } else {
                            migratableEntities.put(id, Sets.newHashSet(restPermission));
                        }
                    }
                });
        return migratableEntities;
    }
}
