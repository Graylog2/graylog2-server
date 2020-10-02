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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.bson.types.ObjectId;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.permissions.GRNPermission;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.Configuration;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.migrations.V20200803120800_GrantsMigrations.UserPermissionsToGrantsMigration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.RoleServiceImpl;
import org.graylog2.users.UserImpl;
import org.graylog2.users.UserServiceImplTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nullable;
import javax.validation.Validator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(GRNExtension.class)
@MongoDBFixtures("MigrateUserPermissionsToGrantsTest.json")
class UserPermissionsToGrantsMigrationTest {
    private UserPermissionsToGrantsMigration migration;
    private RoleService roleService;
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
               GRNRegistry grnRegistry) {
        when(permissions.readerBasePermissions()).thenReturn(ImmutableSet.of());
        when(validator.validate(any())).thenReturn(ImmutableSet.of());

        this.grnRegistry = grnRegistry;

        roleService = new RoleServiceImpl(mongodb.mongoConnection(), mongoJackObjectMapperProvider, permissions, validator);

        this.userSelfEditPermissionCount = new Permissions(ImmutableSet.of()).userSelfEditPermissions("dummy").size();

        dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, grnRegistry);
        userService = new TestUserService(mongodb.mongoConnection());
        DBGrantService dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, grnRegistry);
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

    // An incomplete UserService implementation, that needs fewer dependencies
    public static class TestUserService extends PersistedServiceImpl implements UserService {
        final UserImpl.Factory userFactory;

        protected TestUserService(MongoConnection mongoConnection) {
            super(mongoConnection);
            final Permissions permissions = new Permissions(ImmutableSet.of(new RestPermissions()));
            userFactory = new UserServiceImplTest.UserImplFactory(new Configuration(), permissions);
        }

        @Nullable
        @Override
        public User load(String username) {
            final DBObject query = new BasicDBObject();
            query.put(UserImpl.USERNAME, username);

            final List<DBObject> result = query(UserImpl.class, query);
            if (result == null || result.isEmpty()) {
                return null;
            }

            if (result.size() > 1) {
                final String msg = "There was more than one matching user for username " + username + ". This should never happen.";
                throw new RuntimeException(msg);
            }

            final DBObject userObject = result.get(0);
            final Object userId = userObject.get("_id");

            return userFactory.create((ObjectId) userId, userObject.toMap());
        }

        @Override
        public int delete(String username) {
            return 0;
        }

        @Override
        public User create() {
            return null;
        }

        @Override
        public List<User> loadAll() {
            final DBObject query = new BasicDBObject();
            final List<DBObject> result = query(UserImpl.class, query);

            final List<User> users = Lists.newArrayList();
            for (DBObject dbObject : result) {
                users.add(userFactory.create((ObjectId) dbObject.get("_id"), dbObject.toMap()));
            }

            return users;
        }

        @Override
        public User getAdminUser() {
            return null;
        }

        @Override
        public Optional<User> getRootUser() {
            return Optional.empty();
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public List<User> loadAllForAuthServiceBackend(String authServiceBackendId) {
            return Collections.emptyList();
        }

        @Override
        public Collection<User> loadAllForRole(Role role) {
            final String roleId = role.getId();
            final DBObject query = BasicDBObjectBuilder.start(UserImpl.ROLES, new ObjectId(roleId)).get();

            final List<DBObject> result = query(UserImpl.class, query);
            if (result == null || result.isEmpty()) {
                return Collections.emptySet();
            }
            final Set<User> users = Sets.newHashSetWithExpectedSize(result.size());
            for (DBObject dbObject : result) {
                //noinspection unchecked
                users.add(userFactory.create((ObjectId) dbObject.get("_id"), dbObject.toMap()));
            }
            return users;
        }

        @Override
        public Set<String> getRoleNames(User user) {
            return null;
        }

        @Override
        public List<Permission> getPermissionsForUser(User user) {
            return null;
        }

        @Override
        public List<WildcardPermission> getWildcardPermissionsForUser(User user) {
            return null;
        }

        @Override
        public List<GRNPermission> getGRNPermissionsForUser(User user) {
            return null;
        }

        @Override
        public Set<String> getUserPermissionsFromRoles(User user) {
            return null;
        }

        @Override
        public void dissociateAllUsersFromRole(Role role) {
            final Collection<User> usersInRole = loadAllForRole(role);
            // remove role from any user still assigned
            for (User user : usersInRole) {
                if (user.isLocalAdmin()) {
                    continue;
                }
                final HashSet<String> roles = Sets.newHashSet(user.getRoleIds());
                roles.remove(role.getId());
                user.setRoleIds(roles);
                try {
                    save(user);
                } catch (ValidationException e) {
                    throw new RuntimeException("Unable to remove role " + role.getName() + " from user " + user, e);
                }
            }
        }
    }
}
