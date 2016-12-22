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
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.security.InMemoryRolePermissionResolver;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.security.hashing.SHA1HashPasswordAlgorithm;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb MONGO = newInMemoryMongoDbRule().build();
    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");
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
    private InMemoryRolePermissionResolver permissionsResolver;

    @Before
    public void setUp() throws Exception {
        this.mongoConnection = mongoRule.getMongoConnection();
        this.configuration = new Configuration();
        this.userFactory = new UserImplFactory(configuration);
        this.permissions = new Permissions(ImmutableSet.of(new RestPermissions()));
        this.userService = new UserServiceImpl(mongoConnection, configuration, roleService, userFactory,
                                               permissionsResolver);

        when(roleService.getAdminRoleObjectId()).thenReturn("deadbeef");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testLoad() throws Exception {
        final User user = userService.load("user1");
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("user1");
        assertThat(user.getEmail()).isEqualTo("user1@example.com");
    }

    @Test(expected = RuntimeException.class)
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testLoadDuplicateUser() throws Exception {
        userService.load("user-duplicate");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testDelete() throws Exception {
        assertThat(userService.delete("user1")).isEqualTo(1);
        assertThat(userService.delete("user-duplicate")).isEqualTo(2);
        assertThat(userService.delete("user-does-not-exist")).isEqualTo(0);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testLoadAll() throws Exception {
        assertThat(userService.loadAll()).hasSize(4);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
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
        final UserService userService = new UserServiceImpl(mongoConnection, configuration, roleService, userFactory,
                                                            permissionResolver);

        final UserImplFactory factory = new UserImplFactory(new Configuration());
        final UserImpl user = factory.create(new HashMap<>());
        user.setName("user");
        final Role role = createRole("Foo");

        user.setRoleIds(Collections.singleton(role.getId()));
        user.setPermissions(Collections.singletonList("hello:world"));

        when(permissionResolver.resolveStringPermission(role.getId())).thenReturn(Collections.singleton("foo:bar"));

        assertThat(userService.getPermissionsForUser(user)).containsOnly("users:passwordchange:user", "users:edit:user", "foo:bar", "hello:world");
    }
}
