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
package org.graylog2.users;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.InMemoryRolePermissionResolver;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.security.hashing.SHA1HashPasswordAlgorithm;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserServiceImplTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private MongoConnection mongoConnection;
    private Configuration configuration;
    private UserImpl.Factory userFactory;
    private UserServiceImpl userService;
    private Permissions permissions;
    @Mock
    private RoleService roleService;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private InMemoryRolePermissionResolver permissionsResolver;
    @Mock
    private EventBus serverEventBus;

    @Before
    public void setUp() throws Exception {
        this.mongoConnection = mongodb.mongoConnection();
        this.configuration = new Configuration();
        this.userFactory = new UserImplFactory(configuration);
        this.permissions = new Permissions(ImmutableSet.of(new RestPermissions()));
        this.userService = new UserServiceImpl(mongoConnection, configuration, roleService, accessTokenService,
                                               userFactory, permissionsResolver, serverEventBus);

        when(roleService.getAdminRoleObjectId()).thenReturn("deadbeef");
    }

    @Test
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testLoad() throws Exception {
        final User user = userService.load("user1");
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("user1");
        assertThat(user.getEmail()).isEqualTo("user1@example.com");
    }

    @Test(expected = RuntimeException.class)
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testLoadDuplicateUser() throws Exception {
        userService.load("user-duplicate");
    }

    @Test
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testDelete() throws Exception {
        assertThat(userService.delete("user1")).isEqualTo(1);
        assertThat(userService.delete("user-duplicate")).isEqualTo(2);
        assertThat(userService.delete("user-does-not-exist")).isEqualTo(0);
    }

    @Test
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testLoadAll() throws Exception {
        assertThat(userService.loadAll()).hasSize(4);
    }

    @Test
    public void testSave() throws Exception {
        final User user = userService.create();
        user.setName("TEST");
        user.setFullName("TEST");
        user.setEmail("test@example.com");
        user.setTimeZone(DateTimeZone.UTC);
        user.setPassword("TEST");
        user.setPermissions(Collections.<String>emptyList());

        final String id = userService.save(user);
        final DBObject query = BasicDBObjectBuilder.start("_id", new ObjectId(id)).get();
        @SuppressWarnings("deprecation")
        final DBObject dbObject = mongoConnection.getDatabase().getCollection(UserImpl.COLLECTION_NAME).findOne(query);
        assertThat(dbObject.get("username")).isEqualTo("TEST");
        assertThat(dbObject.get("full_name")).isEqualTo("TEST");
        assertThat(dbObject.get("email")).isEqualTo("test@example.com");
        assertThat(dbObject.get("timezone")).isEqualTo("UTC");
        assertThat((String) dbObject.get("password")).isNotEmpty();
    }

    @Test
    public void testGetAdminUser() throws Exception {
        assertThat(userService.getAdminUser().getName()).isEqualTo(configuration.getRootUsername());
        assertThat(userService.getAdminUser().getEmail()).isEqualTo(configuration.getRootEmail());
        assertThat(userService.getAdminUser().getTimeZone()).isEqualTo(configuration.getRootTimeZone());
    }

    @Test
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testCount() throws Exception {
        assertThat(userService.count()).isEqualTo(4L);
    }

    class UserImplFactory implements UserImpl.Factory {
        private final Configuration configuration;
        private final PasswordAlgorithmFactory passwordAlgorithmFactory;

        public UserImplFactory(Configuration configuration) {
            this.configuration = configuration;
            this.passwordAlgorithmFactory = new PasswordAlgorithmFactory(Collections.<String, PasswordAlgorithm>emptyMap(),
                    new SHA1HashPasswordAlgorithm("TESTSECRET"));
        }

        @Override
        public UserImpl create(Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, fields);
        }

        @Override
        public UserImpl create(ObjectId id, Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, id, fields);
        }

        @Override
        public UserImpl.LocalAdminUser createLocalAdminUser(String adminRoleObjectId) {
            return new UserImpl.LocalAdminUser(passwordAlgorithmFactory, configuration, adminRoleObjectId);
        }
    }

    private Role createRole(String name) {
        final RoleImpl role = new RoleImpl();

        role._id = new ObjectId().toString();
        role.setName(name);

        return role;
    }

    @Test
    public void testGetRoleNames() throws Exception {
        final UserImplFactory factory = new UserImplFactory(new Configuration());
        final UserImpl user = factory.create(new HashMap<>());
        final Role role = createRole("Foo");

        final ImmutableMap<String, Role> map = ImmutableMap.<String, Role>builder()
                .put(role.getId(), role)
                .build();

        when(roleService.loadAllIdMap()).thenReturn(map);
        assertThat(userService.getRoleNames(user)).isEmpty();

        user.setRoleIds(Sets.newHashSet(role.getId()));
        assertThat(userService.getRoleNames(user)).containsOnly("Foo");

        when(roleService.loadAllIdMap()).thenReturn(new HashMap<>());
        assertThat(userService.getRoleNames(user)).isEmpty();
    }

    @Test
    public void testGetPermissionsForUser() throws Exception {
        final InMemoryRolePermissionResolver permissionResolver = mock(InMemoryRolePermissionResolver.class);
        final UserService userService = new UserServiceImpl(mongoConnection, configuration, roleService,
                                                            accessTokenService,userFactory, permissionResolver,
                                                            serverEventBus);

        final UserImplFactory factory = new UserImplFactory(new Configuration());
        final UserImpl user = factory.create(new HashMap<>());
        user.setName("user");
        final Role role = createRole("Foo");

        user.setRoleIds(Collections.singleton(role.getId()));
        user.setPermissions(Collections.singletonList("hello:world"));

        when(permissionResolver.resolveStringPermission(role.getId())).thenReturn(Collections.singleton("foo:bar"));

        assertThat(userService.getPermissionsForUser(user)).containsOnly("users:passwordchange:user", "users:edit:user",
                "foo:bar", "hello:world", "users:tokenlist:user", "users:tokencreate:user", "users:tokenremove:user");
    }
}
