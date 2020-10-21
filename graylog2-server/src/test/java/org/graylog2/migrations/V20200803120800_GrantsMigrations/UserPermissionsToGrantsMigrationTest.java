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

import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.TestUserService;
import org.graylog.testing.TestUserServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(TestUserServiceExtension.class)
@MongoDBFixtures("MigrateUserPermissionsToGrantsTest.json")
class UserPermissionsToGrantsMigrationTest {
    private UserPermissionsToGrantsMigration migration;
    private DBGrantService dbGrantService;

    @Mock
    private Permissions permissions;

    @Mock
    private Validator validator;
    private GRNRegistry grnRegistry;

    private UserService userService;

    private int userSelfEditPermissionCount;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
               GRNRegistry grnRegistry,
               TestUserService userService) {

        this.grnRegistry = grnRegistry;

        this.userSelfEditPermissionCount = new Permissions(ImmutableSet.of()).userSelfEditPermissions("dummy").size();

        dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, grnRegistry, mock(ClusterEventBus.class));
        this.userService = userService;
        DBGrantService dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, grnRegistry, mock(ClusterEventBus.class));
        migration = new UserPermissionsToGrantsMigration(userService, dbGrantService, grnRegistry, "admin");
    }

    @Test
    void migrateAllUserPermissions() throws NotFoundException {

        User testuser1 = userService.load("testuser1");
        assertThat(testuser1).isNotNull();

        assertThat(testuser1.getPermissions().size()).isEqualTo(5 + userSelfEditPermissionCount);
        assertThat(dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser1)))).isEmpty();

        migration.upgrade();

        // check created grants for testuser1
        final ImmutableSet<GrantDTO> grants = dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser1)));
        assertGrantInSet(grants, "grn::::dashboard:5e2afc66cd19517ec2dabadd", Capability.VIEW);
        assertGrantInSet(grants, "grn::::dashboard:5e2afc66cd19517ec2dabadf", Capability.MANAGE);
        assertGrantInSet(grants, "grn::::stream:5c40ad603c034441a56942bd", Capability.VIEW);
        assertGrantInSet(grants, "grn::::stream:5e2f5cfb4868e67ad4da562d", Capability.VIEW);
        assertThat(grants.size()).isEqualTo(4);

        // reload user and check that all migrated permissions have been removed
        testuser1 = userService.load("testuser1");
        assertThat(testuser1).isNotNull();
        assertThat(testuser1.getPermissions().size()).isEqualTo(userSelfEditPermissionCount);
    }

    @Test
    void migrateSomeUserPermissions() throws NotFoundException {

        User testuser2 = userService.load("testuser2");
        assertThat(testuser2).isNotNull();

        assertThat(testuser2.getPermissions().size()).isEqualTo(6 + userSelfEditPermissionCount);
        assertThat(dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser2)))).isEmpty();

        migration.upgrade();

        // check created grants for testuser2
        final ImmutableSet<GrantDTO> grants = dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser2)));
        assertGrantInSet(grants, "grn::::dashboard:5e2afc66cd19517ec2dabadf", Capability.MANAGE);
        assertThat(grants.size()).isEqualTo(1);

        // reload user and check that all migrated permissions have been removed. (should be only two less)
        testuser2 = userService.load("testuser2");
        assertThat(testuser2).isNotNull();
        assertThat(testuser2.getPermissions().size()).isEqualTo(4 + userSelfEditPermissionCount);
    }


    private void assertGrantInSet(Set<GrantDTO> grants, String target, Capability capability) {
        assertThat(grants.stream().anyMatch(g -> g.target().equals(grnRegistry.parse(target)) && g.capability().equals(capability))).isTrue();
    }
}
