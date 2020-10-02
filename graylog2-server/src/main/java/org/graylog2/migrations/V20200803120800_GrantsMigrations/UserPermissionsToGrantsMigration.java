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
package org.graylog2.migrations.V20200803120800_GrantsMigrations;

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
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collection;
import java.util.HashMap;
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

public class UserPermissionsToGrantsMigration {
    private static final Logger LOG = LoggerFactory.getLogger(UserPermissionsToGrantsMigration.class);
    private final UserService userService;
    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;
    private final String rootUsername;

    public UserPermissionsToGrantsMigration(UserService userService,
                                            DBGrantService dbGrantService,
                                            GRNRegistry grnRegistry,
                                            @Named("root_username") String rootUsername) {
        this.userService = userService;
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
        this.rootUsername = rootUsername;
    }

    private final Map<Set<String>, GRNTypeCapability> MIGRATION_MAP = ImmutableMap.of(
            ImmutableSet.of(DASHBOARDS_READ, DASHBOARDS_EDIT), new GRNTypeCapability(DASHBOARD, Capability.MANAGE),
            ImmutableSet.of(DASHBOARDS_READ), new GRNTypeCapability(DASHBOARD, Capability.VIEW),
            ImmutableSet.of(STREAMS_READ, STREAMS_EDIT), new GRNTypeCapability(STREAM, Capability.MANAGE),
            ImmutableSet.of(STREAMS_READ), new GRNTypeCapability(STREAM, Capability.VIEW)
            );

    public void upgrade() {
        final List<User> users = userService.loadAll();

        for (User user : users) {
            final Map<String, Set<String>> migratableEntities = getMigratableEntities(ImmutableSet.copyOf(user.getPermissions()));
            if (!migratableEntities.isEmpty()) {
                migrateUserPermissions(user, migratableEntities);
            }
        }
    }

    private void migrateUserPermissions(User user, Map<String, Set<String>> migratableEntities) {
        migratableEntities.forEach((entityID, permissions) -> {
            final GRNTypeCapability grnTypeCapability = MIGRATION_MAP.get(permissions);

            // Permissions are mappable to a grant
            if (grnTypeCapability != null) {
                final Capability capability = grnTypeCapability.capability;
                final GRNType grnType = grnTypeCapability.grnType;
                final GrantDTO grant = GrantDTO.builder()
                        .grantee(grnRegistry.ofUser(user))
                        .target(grnType.toGRN(entityID))
                        .capability(capability)
                        .build();
                dbGrantService.create(grant, rootUsername);

                final List<String> updatedPermissions = user.getPermissions();
                updatedPermissions.removeAll(permissions.stream().map(p -> p + ":" + entityID).collect(Collectors.toSet()));
                user.setPermissions(updatedPermissions);
                try {
                    userService.save(user);
                } catch (ValidationException e) {
                    LOG.error("Failed to update permssions on user <{}>", user.getName(), e);
                }
                LOG.info("Migrating entity <{}> permissions <{}> to <{}> grant for user <{}>", entityID, permissions, capability, user.getName());
            } else {
                LOG.info("Skipping non-migratable entity <{}>. Permissions <{}> cannot be converted to a grant capability", entityID, permissions);
            }
        });
    }

    private Map<String, Set<String>> getMigratableEntities(Set<String> permissions) {
        Map<String, Set<String>> migratableEntities = new HashMap<>();

        permissions.stream().map(MigrationWildcardPermission::new)
                .filter(p -> p.getParts().size() == 3 && p.getParts().stream().allMatch(part -> part.size() == 1))
                .forEach(p -> {
                    String permissionType = p.subPart(0);
                    String restPermission = p.subPart(0) + ":" + p.subPart(1);
                    String id = p.subPart(2);

                    if (MIGRATION_MAP.keySet().stream().flatMap(Collection::stream).anyMatch(perm -> perm.startsWith(permissionType + ":"))) {
                        if (migratableEntities.containsKey(id)) {
                            migratableEntities.get(id).add(restPermission);
                        } else {
                            migratableEntities.put(id, Sets.newHashSet(restPermission));
                        }
                    }
                });
        return migratableEntities;
    }

    private static class GRNTypeCapability {
        final GRNType grnType;
        final Capability capability;

        public GRNTypeCapability(GRNType grnType, Capability capability) {
            this.grnType = grnType;
            this.capability = capability;
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
