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
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.graylog.grn.GRNTypes.DASHBOARD;
import static org.graylog.grn.GRNTypes.EVENT_DEFINITION;
import static org.graylog.grn.GRNTypes.SEARCH;
import static org.graylog.grn.GRNTypes.STREAM;
import static org.graylog.plugins.views.search.rest.ViewsRestPermissions.VIEW_EDIT;
import static org.graylog.plugins.views.search.rest.ViewsRestPermissions.VIEW_READ;
import static org.graylog2.shared.security.RestPermissions.DASHBOARDS_EDIT;
import static org.graylog2.shared.security.RestPermissions.DASHBOARDS_READ;
import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_EDIT;
import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_READ;
import static org.graylog2.shared.security.RestPermissions.STREAMS_EDIT;
import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;

public class GrantsMetaMigration extends Migration {
    private final RoleService roleService;
    private final UserService userService;
    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;
    private final String rootUsername;
    private final MongoConnection mongoConnection;
    private final ViewService viewService;

    @Inject
    public GrantsMetaMigration(RoleService roleService,
                               UserService userService,
                               DBGrantService dbGrantService,
                               GRNRegistry grnRegistry,
                               @Named("root_username") String rootUsername,
                               MongoConnection mongoConnection,
                               ViewService viewService) {
        this.roleService = roleService;
        this.userService = userService;
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
        this.rootUsername = rootUsername;
        this.mongoConnection = mongoConnection;
        this.viewService = viewService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-08-03T12:08:00Z");
    }

    public static final Map<Set<String>, GRNTypeCapability> MIGRATION_MAP = ImmutableMap.<Set<String>, GRNTypeCapability>builder()
            .put(ImmutableSet.of(DASHBOARDS_READ, DASHBOARDS_EDIT), new GRNTypeCapability(DASHBOARD, Capability.MANAGE))
            .put(ImmutableSet.of(DASHBOARDS_READ), new GRNTypeCapability(DASHBOARD, Capability.VIEW))
            .put(ImmutableSet.of(STREAMS_READ, STREAMS_EDIT), new GRNTypeCapability(STREAM, Capability.MANAGE))
            .put(ImmutableSet.of(STREAMS_READ), new GRNTypeCapability(STREAM, Capability.VIEW))
            .put(ImmutableSet.of(VIEW_READ, VIEW_EDIT), new GRNTypeCapability(SEARCH, Capability.MANAGE))
            .put(ImmutableSet.of(VIEW_READ), new GRNTypeCapability(SEARCH, Capability.VIEW))
            .put(ImmutableSet.of(EVENT_DEFINITIONS_READ, EVENT_DEFINITIONS_EDIT), new GRNTypeCapability(EVENT_DEFINITION, Capability.MANAGE))
            .put(ImmutableSet.of(EVENT_DEFINITIONS_READ), new GRNTypeCapability(EVENT_DEFINITION, Capability.VIEW))
            .build();

    @Override
    public void upgrade() {
        // ViewSharingToGrantsMigration needs to run before the RolesToGrantsMigration drops empty roles
        new ViewSharingToGrantsMigration(mongoConnection, dbGrantService, userService, roleService, rootUsername, viewService, grnRegistry).upgrade();
        new RolesToGrantsMigration(roleService, userService, dbGrantService, grnRegistry, rootUsername).upgrade();
        new ViewOwnerShipToGrantsMigration(userService, dbGrantService, rootUsername, viewService, grnRegistry).upgrade();
        new UserPermissionsToGrantsMigration(userService, dbGrantService, grnRegistry, rootUsername).upgrade();
    }

    public static class GRNTypeCapability {
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
