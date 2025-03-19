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
package org.graylog2.rest.resources.users;

import com.google.common.collect.ImmutableSet;
import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.Configuration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.MongoDBSessionService;
import org.graylog2.security.UserSessionTerminationService;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserManagementService;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserConfiguration;
import org.graylog2.users.UserImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.graylog2.shared.security.RestPermissions.USERS_TOKENCREATE;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class UserResourceGenerateTokenAccessTest {

    private static final String USERNAME = "username";

    private static final String ADMIN_OBJECT_ID = new ObjectId().toHexString();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private UsersResource usersResource;
    @Mock
    private UserService userService;
    @Mock
    private PaginatedUserService paginatedUserService;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private RoleService roleService;
    @Mock
    private MongoDBSessionService sessionService;
    @Mock
    private Subject subject;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private UserSessionTerminationService sessionTerminationService;
    @Mock
    private DefaultSecurityManager securityManager;
    @Mock
    private GlobalAuthServiceConfig globalAuthServiceConfig;
    @Mock
    private ClusterConfigService clusterConfigService;

    private UsersResourceTest.UserImplFactory userImplFactory;

    @Before
    public void setUp() {
        userImplFactory = new UsersResourceTest.UserImplFactory(new Configuration(),
                new Permissions(ImmutableSet.of(new RestPermissions())));
        usersResource = new UsersResourceTest.TestUsersResource(userManagementService, paginatedUserService, accessTokenService,
                roleService, sessionService, new HttpConfiguration(), subject,
                sessionTerminationService, securityManager, globalAuthServiceConfig, clusterConfigService, userService);
    }

    @Parameterized.Parameters(name = "{index}: permitted: {0}, external: {1}, admin: {2}, confAllowExternals: {3}, confOnlyAdmin: {4} => allowed: {5}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                //Row's meaning:
                //RestPermission, External, Admin, confExternalOk, confOnlyAdmin, allowed
                // Admin is always allowed:
                {true, true, true, true, true, true},
                {true, true, true, true, false, true},
                {true, true, true, false, true, true},
                {true, true, true, false, false, true},
                {true, false, true, true, true, true},
                {true, false, true, true, false, true},
                {true, false, true, false, true, true},
                {true, false, true, false, false, true},
                // Remaining cases:
                {true, true, false, true, true, false},
                {true, true, false, true, false, true},
                {true, true, false, false, true, false},
                {true, true, false, false, false, false},
                {true, false, false, true, true, false},
                {true, false, false, true, false, true},
                {true, false, false, false, true, false},
                {true, false, false, false, false, true},

                //No matter what - no permissions, not allowed:
                {false, true, true, true, true, false},
                {false, true, true, true, false, false},
                {false, true, true, false, true, false},
                {false, true, true, false, false, false},
                {false, true, false, true, true, false},
                {false, true, false, true, false, false},
                {false, true, false, false, true, false},
                {false, true, false, false, false, false},
                {false, false, true, true, true, false},
                {false, false, true, true, false, false},
                {false, false, true, false, true, false},
                {false, false, true, false, false, false},
                {false, false, false, true, true, false},
                {false, false, false, true, false, false},
                {false, false, false, false, true, false},
                {false, false, false, false, false, false}
        });
    }

    private final boolean isPermitted;
    private final boolean isExternal;
    private final boolean isAdmin;
    private final boolean confAllowExternal;
    private final boolean confDenyNonAdmins;
    private final boolean expectedResult;


    public UserResourceGenerateTokenAccessTest(boolean isPermitted, boolean isExternal, boolean isAdmin, boolean confAllowExternal, boolean confDenyNonAdmins, boolean expectedResult) {
        this.isPermitted = isPermitted;
        this.isExternal = isExternal;
        this.isAdmin = isAdmin;
        this.confAllowExternal = confAllowExternal;
        this.confDenyNonAdmins = confDenyNonAdmins;
        this.expectedResult = expectedResult;
    }

    private User mkUser() {
        final Map<String, Object> userProps = new HashMap<>();
        userProps.put(UserImpl.USERNAME, USERNAME);
        userProps.put(UserImpl.EXTERNAL_USER, this.isExternal);
        final User result = userImplFactory.create(userProps);
        if (isAdmin) {
            result.setRoleIds(Set.of(ADMIN_OBJECT_ID));
        }
        return result;
    }

    @Test
    public void testAccess() {
        final User user = mkUser();
        prepareMocks();
        if (expectedResult) {
            usersResource.validatePermissionForTokenCreation(user, user);
        } else {
            assertThrows(ForbiddenException.class, () -> usersResource.validatePermissionForTokenCreation(user, user));
        }
    }

    private void prepareMocks() {
        when(roleService.getAdminRoleObjectId()).thenReturn(ADMIN_OBJECT_ID);
        when(subject.isPermitted(USERS_TOKENCREATE + ":" + USERNAME)).thenReturn(isPermitted);
        if (!isAdmin) {
            when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES))
                    .thenReturn(UserConfiguration.create(false, Duration.of(8, ChronoUnit.HOURS), confAllowExternal, confDenyNonAdmins, Duration.ofDays(30)));
        }
    }
}
