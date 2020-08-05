/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.migrations;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.grn.GRNTypes.DASHBOARD;
import static org.graylog.grn.GRNTypes.STREAM;
import static org.graylog2.shared.security.RestPermissions.DASHBOARDS_EDIT;
import static org.graylog2.shared.security.RestPermissions.DASHBOARDS_READ;
import static org.graylog2.shared.security.RestPermissions.STREAMS_EDIT;
import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;

public class V20200803120800_MigrateRolesToGrants extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20200803120800_MigrateRolesToGrants.class);
    private final RoleService roleService;
    private final UserService userService;
    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;
    private final String rootUsername;

    @Inject
    public V20200803120800_MigrateRolesToGrants(RoleService roleService,
                                                UserService userService,
                                                DBGrantService dbGrantService,
                                                GRNRegistry grnRegistry,
                                                @Named("root_username") String rootUsername) {
        this.roleService = roleService;
        this.userService = userService;
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
        this.rootUsername = rootUsername;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-08-03T12:08:00Z");
    }

    private final Map<Set<String>, GRNTypeCapability> MIGRATION_MAP = ImmutableMap.of(
            ImmutableSet.of(DASHBOARDS_READ, DASHBOARDS_EDIT), new GRNTypeCapability(DASHBOARD, Capability.MANAGE),
            ImmutableSet.of(DASHBOARDS_READ), new GRNTypeCapability(DASHBOARD, Capability.VIEW),
            ImmutableSet.of(STREAMS_READ, STREAMS_EDIT), new GRNTypeCapability(STREAM, Capability.MANAGE),
            ImmutableSet.of(STREAMS_READ), new GRNTypeCapability(STREAM, Capability.VIEW)
            );

    @Override
    public void upgrade() {
        final Set<MigratableRole> migratableRoles = findMigratableRoles();

        migratableRoles.forEach(migratableRole -> {
            final Role role = migratableRole.role;

            final Set<String> migratedPermissions = migrateRoleToGrant(migratableRole);

            if (role.getPermissions().removeAll(migratedPermissions)) {
                LOG.debug("Updating role <{}> new permissions: <{}>", role.getName(), role.getPermissions());

                if (role.getPermissions().isEmpty()) {
                    LOG.info("Removing the now empty role <{}>", role.getName());
                    userService.dissociateAllUsersFromRole(role);
                    roleService.delete(role.getName());
                } else {
                    try {
                        roleService.save(role);
                    } catch (ValidationException e) {
                        LOG.error("Failed to update modified role <{}>", role.getName(), e);
                    }
                }
            }
        });
    }

    private Set<String> migrateRoleToGrant(MigratableRole migratableRole) {
        final Set<String> migratedRolePermissions = new HashSet<>();
        final Collection<User> allRoleUsers = userService.loadAllForRole(migratableRole.role);

        migratableRole.migratableEntities.forEach((entityID, permissions) -> {
            final GRNTypeCapability grnTypeCapability = MIGRATION_MAP.get(permissions);

            // Permissions are mappable to a grant
            if (grnTypeCapability != null) {
                final Capability capability = grnTypeCapability.capability;
                final GRNType grnType = grnTypeCapability.grnType;
                allRoleUsers.forEach(user -> {
                    final GrantDTO grant = GrantDTO.builder()
                            .grantee(grnRegistry.ofUser(user))
                            .target(grnType.toGRN(entityID))
                            .capability(capability)
                            .build();
                    dbGrantService.create(grant, rootUsername);
                    LOG.info("Migrating entity <{}> permissions <{}> to <{}> grant for user <{}>", entityID, permissions, capability, user.getName());
                });
                migratedRolePermissions.addAll(permissions.stream().map(p -> p + ":" + entityID).collect(Collectors.toSet()));
            } else {
                LOG.info("Skipping non-migratable entity <{}>. Permissions <{}> cannot be converted to a grant capability", entityID, permissions);
            }
        });
        return migratedRolePermissions;
    }

    private Set<MigratableRole> findMigratableRoles() {
        final Set<MigratableRole> migratableRoles= new HashSet<>();

        final Set<Role> roles = roleService.loadAll();
        roles.forEach(role -> {
            final Map<String, Set<String>> migratableIds = new HashMap<>();

            // Inspect all permissions that are made of 3 parts and don't contain multiple subparts
            role.getPermissions().stream().map(MigrationWildcardPermission::new)
                    .filter(p -> p.getParts().size() == 3 && p.getParts().stream().allMatch(part -> part.size() == 1))
                    .forEach(p -> {
                        String permissionType = p.subPart(0);
                        String restPermission = p.subPart(0) + ":" + p.subPart(1);
                        String id = p.subPart(2);

                        if (MIGRATION_MAP.keySet().stream().flatMap(Collection::stream).anyMatch(perm -> perm.startsWith(permissionType + ":"))) {
                            LOG.debug("Potentially migratable role <{}> permission <{}> id <{}>", role.getName(), restPermission, id);
                            if (migratableIds.containsKey(id)) {
                                migratableIds.get(id).add(restPermission);
                            } else {
                                migratableIds.put(id, Sets.newHashSet(restPermission));
                            }
                        }
                    });
            if (!migratableIds.isEmpty()) {
                migratableRoles.add(new MigratableRole(role, migratableIds));
            }
        });
        LOG.debug("migratableRoles <{}>", migratableRoles);
        return migratableRoles;
    }

    private static class GRNTypeCapability {
        final GRNType grnType;
        final Capability capability;

        public GRNTypeCapability(GRNType grnType, Capability capability) {
            this.grnType = grnType;
            this.capability = capability;
        }
    }

    private static class MigratableRole {
        Role role;
        Map<String, Set<String>> migratableEntities;

        public MigratableRole(Role role, Map<String, Set<String>> migratableEntities) {
            this.role = role;
            this.migratableEntities = migratableEntities;
        }

        @Override
        public String toString() {
            return "MigratableRole{" +
                    "roleID='" + role.getId() + '\'' +
                    ", migratableIds=" + migratableEntities +
                    '}';
        }
    }

    // only needed to access protected getParts() method from WildcardPermission
    public static class MigrationWildcardPermission extends WildcardPermission {
        public MigrationWildcardPermission(String wildcardString) {
            super(wildcardString);
        }

        @Override
        protected List<Set<String>> getParts() {
            return super.getParts();
        }

        protected String subPart(int idx) {
            return Iterables.getOnlyElement(getParts().get(idx));
        }
    }
}
