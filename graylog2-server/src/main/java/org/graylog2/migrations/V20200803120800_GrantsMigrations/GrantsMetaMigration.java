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

import org.graylog.grn.GRNRegistry;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.DBGrantService;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;

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

    @Override
    public void upgrade() {
        // ViewSharingToGrantsMigration needs to run first, so empty roles get dropped
        new ViewSharingToGrantsMigration(mongoConnection, dbGrantService, userService, roleService, rootUsername, viewService).upgrade();
        new RolesToGrantsMigration(roleService, userService, dbGrantService, grnRegistry, rootUsername).upgrade();
        new ViewOwnerShipToGrantsMigration(userService, dbGrantService, rootUsername, viewService).upgrade();
    }
}
