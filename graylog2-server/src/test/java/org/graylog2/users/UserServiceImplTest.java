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
package org.graylog2.users;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.bson.types.ObjectId;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog.security.permissions.GRNPermission;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock
    private PermissionAndRoleResolver permissionAndRoleResolver;

    @Before
    public void setUp() throws Exception {
        this.mongoConnection = mongodb.mongoConnection();
        this.configuration = new Configuration();
        this.permissions = new Permissions(ImmutableSet.of(new RestPermissions()));
        this.userFactory = new UserImplFactory(configuration, permissions);
        this.userService = new UserServiceImpl(mongoConnection, configuration, roleService, accessTokenService,
                userFactory, permissionsResolver, serverEventBus, GRNRegistry.createWithBuiltinTypes(), permissionAndRoleResolver);

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

    @Test
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testLoadByUserId() throws Exception {
        final User user = userService.loadById("54e3deadbeefdeadbeef0001");
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("54e3deadbeefdeadbeef0001");
        assertThat(user.getName()).isEqualTo("user1");
        assertThat(user.getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testLoadByUserIds() throws Exception {
        final List<User> users = userService.loadByIds(ImmutableSet.of(
                "54e3deadbeefdeadbeef0001",
                "54e3deadbeefdeadbeef0002",
                UserImpl.LocalAdminUser.LOCAL_ADMIN_ID
        ));

        assertThat(users).hasSize(3);

        assertThat(users.get(0).getId()).isEqualTo("local:admin");
        assertThat(users.get(0).getName()).isEqualTo("admin");
        assertThat(users.get(0).getEmail()).isEmpty();

        assertThat(users.get(1).getId()).isEqualTo("54e3deadbeefdeadbeef0001");
        assertThat(users.get(1).getName()).isEqualTo("user1");
        assertThat(users.get(1).getEmail()).isEqualTo("user1@example.com");

        assertThat(users.get(2).getId()).isEqualTo("54e3deadbeefdeadbeef0002");
        assertThat(users.get(2).getName()).isEqualTo("user2");
        assertThat(users.get(2).getEmail()).isEqualTo("user2@example.com");
    }

    @Test
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testLoadByUserIdsWithAdminOnly() throws Exception {
        final List<User> users = userService.loadByIds(ImmutableSet.of(
                UserImpl.LocalAdminUser.LOCAL_ADMIN_ID
        ));

        assertThat(users).hasSize(1);

        assertThat(users.get(0).getId()).isEqualTo("local:admin");
        assertThat(users.get(0).getName()).isEqualTo("admin");
        assertThat(users.get(0).getEmail()).isEmpty();
    }

    @Test(expected = RuntimeException.class)
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testLoadDuplicateUser() throws Exception {
        userService.load("user-duplicate");
    }

    @Test
    @MongoDBFixtures("UserServiceImplTest.json")
    public void testLoadByAuthServiceUidOrUsername() throws Exception {
        final Optional<User> byAuthServiceUid = userService.loadByAuthServiceUidOrUsername("NmIxY2E3ZWQtMTk3NC00NGM4LTkwOTYtN2Q3OTBlM2Y2MjRmCg==", "external-user1");

        assertThat(byAuthServiceUid).get().satisfies(user -> {
            assertThat(user.getAuthServiceId()).isEqualTo("54e3deadbeefdeadbeef0001");
            assertThat(user.getAuthServiceUid()).isEqualTo("NmIxY2E3ZWQtMTk3NC00NGM4LTkwOTYtN2Q3OTBlM2Y2MjRmCg==");
            assertThat(user.getEmail()).isEqualTo("external-user1@example.com");
        });

        final Optional<User> byUsername = userService.loadByAuthServiceUidOrUsername("__nope__", "external-user1");

        assertThat(byUsername).get().satisfies(user -> {
            assertThat(user.getAuthServiceId()).isEqualTo("54e3deadbeefdeadbeef0001");
            assertThat(user.getAuthServiceUid()).isEqualTo("NmIxY2E3ZWQtMTk3NC00NGM4LTkwOTYtN2Q3OTBlM2Y2MjRmCg==");
            assertThat(user.getEmail()).isEqualTo("external-user1@example.com");
        });

        assertThat(userService.loadByAuthServiceUidOrUsername("__nope__", "__nope__"))
                .isNotPresent();

        assertThatThrownBy(() -> userService.loadByAuthServiceUidOrUsername(null, "__nope__"))
                .hasMessageContaining("authServiceUid")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.loadByAuthServiceUidOrUsername("__nope__", null))
                .hasMessageContaining("username")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.loadByAuthServiceUidOrUsername(null, null))
                .isInstanceOf(IllegalArgumentException.class);
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
        assertThat(userService.loadAll()).hasSize(5);
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
    public void testSaveValidationFullName() throws Exception {
        final User user = userService.create();
        user.setName("TEST");
        user.setEmail("test@example.com");
        user.setTimeZone(DateTimeZone.UTC);
        user.setPassword("TEST");
        user.setPermissions(Collections.<String>emptyList());
        user.setFullName(null);

        assertThatThrownBy(() -> userService.save(user))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A fullName is required.");
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
        assertThat(userService.count()).isEqualTo(5L);
    }

    public static class UserImplFactory implements UserImpl.Factory {
        private final Configuration configuration;
        private final Permissions permissions;
        private final PasswordAlgorithmFactory passwordAlgorithmFactory;

        public UserImplFactory(Configuration configuration, Permissions permissions) {
            this.configuration = configuration;
            this.permissions = permissions;
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
        final UserImplFactory factory = new UserImplFactory(new Configuration(), permissions);
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
        final GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();
        final UserService userService = new UserServiceImpl(mongoConnection, configuration, roleService,
                accessTokenService, userFactory, permissionResolver,
                serverEventBus, grnRegistry, permissionAndRoleResolver);

        final UserImplFactory factory = new UserImplFactory(new Configuration(), permissions);
        final UserImpl user = factory.create(new HashMap<>());
        user.setName("user");
        final Role role = createRole("Foo");

        user.setRoleIds(Collections.singleton(role.getId()));
        user.setPermissions(Collections.singletonList("hello:world"));

        when(permissionResolver.resolveStringPermission(role.getId())).thenReturn(Collections.singleton("foo:bar"));
        final GRNPermission ownerShipPermission = GRNPermission.create(RestPermissions.ENTITY_OWN, grnRegistry.newGRN(GRNTypes.DASHBOARD, "1234"));
        final GRN userGRN = grnRegistry.ofUser(user);
        when(permissionAndRoleResolver.resolvePermissionsForPrincipal(userGRN))
                .thenReturn(ImmutableSet.of(
                        new WildcardPermission("perm:from:grant"),
                        ownerShipPermission));

        final String roleId = "12345";
        when(permissionAndRoleResolver.resolveRolesForPrincipal(userGRN)).thenReturn(ImmutableSet.of(roleId));
        when(permissionResolver.resolveStringPermission(roleId)).thenReturn(ImmutableSet.of("perm:from:role"));

        assertThat(userService.getPermissionsForUser(user).stream().map(p -> p instanceof WildcardPermission ? p.toString() : p).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("users:passwordchange:user", "users:edit:user", "foo:bar", "hello:world", "users:tokenlist:user",
                        "users:tokencreate:user", "users:tokenremove:user", "perm:from:grant", ownerShipPermission, "perm:from:role");
    }
}
