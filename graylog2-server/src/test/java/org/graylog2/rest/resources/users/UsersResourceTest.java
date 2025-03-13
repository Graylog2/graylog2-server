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
import jakarta.ws.rs.core.Response;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.Configuration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.users.requests.CreateUserRequest;
import org.graylog2.rest.models.users.requests.Startpage;
import org.graylog2.rest.models.users.requests.UpdateUserPreferences;
import org.graylog2.rest.models.users.responses.Token;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.MongoDBSessionService;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.security.UserSessionTerminationService;
import org.graylog2.security.hashing.SHA1HashPasswordAlgorithm;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserManagementService;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserConfiguration;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.shared.security.RestPermissions.USERS_TOKENCREATE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UsersResourceTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String EMAIL = "test@graylog.com";
    private static final String FIRST_NAME = "First";
    private static final String LAST_NAME = "Last";
    private static final String TIMEZONE = "Europe/Berlin";
    private static final long SESSION_TIMEOUT = 0L;
    private static final String TOKEN_NAME = "tokenName";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Mock
    private UsersResource usersResource;
    @Mock
    private PaginatedUserService paginatedUserService;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private RoleService roleService;
    @Mock
    private MongoDBSessionService sessionService;
    @Mock
    private Startpage startPage;
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

    UserImplFactory userImplFactory;

    @Before
    public void setUp() {
        userImplFactory = new UserImplFactory(new Configuration(),
                new Permissions(ImmutableSet.of(new RestPermissions())));
        usersResource = new TestUsersResource(userManagementService, paginatedUserService, accessTokenService,
                roleService, sessionService, new HttpConfiguration(), subject,
                sessionTerminationService, securityManager, globalAuthServiceConfig, clusterConfigService);
    }

    /**
     * Verify user is successfully created in the Create flow.
     * This tests the integration between the UsersResource and UserManagementServiceImpl.
     */
    @Test
    public void createSuccess() throws ValidationException {
        when(userManagementService.create()).thenReturn(userImplFactory.create(new HashMap<>()));
        final Response response = usersResource.create(buildCreateUserRequest());
        Assert.assertEquals(201, response.getStatus());
        verify(userManagementService, times(1)).create(isA(UserImpl.class));
    }

    @Test
    public void savePreferencesSuccess() throws ValidationException {
        when(subject.isPermitted(anyString())).thenReturn(true);
        when(userManagementService.load(USERNAME)).thenReturn(userImplFactory.create(new HashMap<>()));
        usersResource.savePreferences(USERNAME, UpdateUserPreferences.create(new HashMap<>()));
        verify(userManagementService, times(1)).save(isA(UserImpl.class));
    }

    @Test
    public void createTokenForInternalUserSucceeds() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME);
        final Token expected = createTokenAndPrepareMocks(userProps, false);

        try {
            final Token actual = usersResource.generateNewToken(USERNAME, UsersResourceTest.TOKEN_NAME, new UsersResource.GenerateTokenTTL(Optional.of(Duration.ofDays(30))));
            assertEquals(expected, actual);
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verify(accessTokenService).create(USERNAME, UsersResourceTest.TOKEN_NAME, Duration.ofDays(30));
            verify(clusterConfigService).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    public void createTokenForInternalUserWithoutTTLSucceedsAndLoadsConfig() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME);
        final Token expected = createTokenAndPrepareMocks(userProps, false);

        try {
            final Token actual = usersResource.generateNewToken(USERNAME, UsersResourceTest.TOKEN_NAME, new UsersResource.GenerateTokenTTL(Optional.empty()));
            assertEquals(expected, actual);
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            //Before calling the service, the configuration for the default TTL is already loaded in the resource:
            verify(accessTokenService).create(USERNAME, UsersResourceTest.TOKEN_NAME, Duration.ofDays(30));
            verify(clusterConfigService, times(2)).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test(expected = ForbiddenException.class)
    public void createTokenForExternalUserFailsIfConfigured() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME, UserImpl.EXTERNAL_USER, "TRUE");

        prepareMocks(userProps, null, false);

        try {
            usersResource.generateNewToken(USERNAME, TOKEN_NAME, new UsersResource.GenerateTokenTTL(Optional.empty()));
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verify(clusterConfigService, times(2)).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    public void createTokenForExternalUserSucceedsIfConfigured() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME, UserImpl.EXTERNAL_USER, "TRUE");
        final Token expected = createTokenAndPrepareMocks(userProps, true);

        try {
            final Token actual = usersResource.generateNewToken(USERNAME, TOKEN_NAME, new UsersResource.GenerateTokenTTL(Optional.of(Duration.ofDays(30))));
            assertEquals(expected, actual);
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verify(clusterConfigService, times(2)).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
            verify(accessTokenService).create(USERNAME, TOKEN_NAME, Duration.ofDays(30));
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    public void createTokenSucceedsEvenWithNULLBody() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME, UserImpl.EXTERNAL_USER, "FALSE");
        final Token expected = createTokenAndPrepareMocks(userProps, true);

        try {
            final Token actual = usersResource.generateNewToken(USERNAME, TOKEN_NAME, null);
            assertEquals(expected, actual);
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verify(clusterConfigService, times(2)).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
            verify(accessTokenService).create(USERNAME, TOKEN_NAME, Duration.ofDays(30));
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    private CreateUserRequest buildCreateUserRequest() {
        return CreateUserRequest.create(USERNAME, PASSWORD, EMAIL,
                FIRST_NAME, LAST_NAME, Collections.singletonList(""),
                TIMEZONE, SESSION_TIMEOUT,
                startPage, Collections.emptyList(), false);
    }

    private Token createTokenAndPrepareMocks(Map<String, Object> userProps, boolean allowExternalUser) {
        final String token = "someToken";
        final DateTime lastAccess = Tools.nowUTC();
        final Map<String, Object> tokenProps = Map.of(AccessTokenImpl.NAME, TOKEN_NAME, AccessTokenImpl.TOKEN, token, AccessTokenImpl.LAST_ACCESS, lastAccess);
        final ObjectId tokenId = new ObjectId();
        final AccessToken accessToken = new AccessTokenImpl(tokenId, tokenProps);

        prepareMocks(userProps, accessToken, allowExternalUser);

        return Token.create(tokenId.toHexString(), TOKEN_NAME, token, lastAccess);
    }

    private void prepareMocks(Map<String, Object> userProps, AccessToken accessToken, boolean allowExternalUser) {
        when(userManagementService.loadById(USERNAME)).thenReturn(userImplFactory.create(userProps));
        when(subject.isPermitted(USERS_TOKENCREATE + ":" + USERNAME)).thenReturn(true);
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES))
                .thenReturn(UserConfiguration.create(false, Duration.of(8, ChronoUnit.HOURS), allowExternalUser, false, Duration.ofDays(30)));
        if (accessToken != null) {
            when(accessTokenService.create(USERNAME, UsersResourceTest.TOKEN_NAME, Duration.ofDays(30))).thenReturn(accessToken);
        }
    }

    /**
     * Test implementation of UsersResource is needed to set superclass configuration property
     * (which is directly injected without a constructor).
     */
    public static class TestUsersResource extends UsersResource {

        private final Subject subject;

        public TestUsersResource(UserManagementService userManagementService, PaginatedUserService paginatedUserService,
                                 AccessTokenService accessTokenService, RoleService roleService,
                                 MongoDBSessionService sessionService, HttpConfiguration configuration,
                                 Subject subject, UserSessionTerminationService sessionTerminationService,
                                 DefaultSecurityManager securityManager, GlobalAuthServiceConfig globalAuthServiceConfig,
                                 ClusterConfigService clusterConfigService) {
            super(userManagementService, paginatedUserService, accessTokenService, roleService, sessionService,
                    sessionTerminationService, securityManager, globalAuthServiceConfig, clusterConfigService);
            this.subject = subject;
            super.configuration = configuration;
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }
    }

    public static class UserImplFactory implements UserImpl.Factory {
        private final Permissions permissions;
        private final PasswordAlgorithmFactory passwordAlgorithmFactory;

        public UserImplFactory(Configuration configuration, Permissions permissions) {
            this.permissions = permissions;
            this.passwordAlgorithmFactory = new PasswordAlgorithmFactory(Collections.emptyMap(),
                    new SHA1HashPasswordAlgorithm("TESTSECRET"));
        }

        @Override
        public UserImpl create(Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, mock(ClusterConfigService.class), fields);
        }

        @Override
        public UserImpl create(ObjectId id, Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, mock(ClusterConfigService.class), id, fields);
        }

        // Not used.
        @Override
        public UserImpl.LocalAdminUser createLocalAdminUser(String adminRoleObjectId) {
            return null;
        }
    }
}
