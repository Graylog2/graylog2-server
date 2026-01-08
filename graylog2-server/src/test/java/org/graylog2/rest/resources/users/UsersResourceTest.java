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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.graylog.security.UserContext;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.users.requests.CreateUserRequest;
import org.graylog2.rest.models.users.requests.DashboardStartPage;
import org.graylog2.rest.models.users.requests.UpdateUserPreferences;
import org.graylog2.rest.models.users.responses.Token;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.security.UserSessionTerminationService;
import org.graylog2.security.hashing.SHA1HashPasswordAlgorithm;
import org.graylog2.security.sessions.SessionService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserManagementService;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.PasswordComplexityConfig;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserConfiguration;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.threeten.extra.PeriodDuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.security.RestPermissions.USERS_TOKENCREATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class UsersResourceTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String EMAIL = "test@graylog.com";
    private static final String FIRST_NAME = "First";
    private static final String LAST_NAME = "Last";
    private static final String TIMEZONE = "Europe/Berlin";
    private static final long SESSION_TIMEOUT = 0L;
    private static final String TOKEN_NAME = "tokenName";

    private static final String ADMIN_OBJECT_ID = new ObjectId().toHexString();
    public static final String ALLOWED_ROLE_LOWER_CASE = TestUsersResource.ALLOWED_ROLE.toLowerCase(Locale.US);

    @Mock
    private UsersResource usersResource;
    @Mock
    private PaginatedUserService paginatedUserService;
    @Mock
    private UserService userService;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private RoleService roleService;
    @Mock
    private SessionService sessionService;
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
    @Mock
    private UserContext userContext;
    @Mock
    private User user;

    UserImplFactory userImplFactory;

    @BeforeEach
    void setUp() {
        userImplFactory = new UserImplFactory(new Permissions(ImmutableSet.of(new RestPermissions())), clusterConfigService);
        usersResource = new TestUsersResource(userManagementService, paginatedUserService, accessTokenService,
                roleService, sessionService, new HttpConfiguration(), subject,
                sessionTerminationService, securityManager, globalAuthServiceConfig, clusterConfigService, userService);
        lenient().when(clusterConfigService.getOrDefault(PasswordComplexityConfig.class, PasswordComplexityConfig.DEFAULT)).thenReturn(PasswordComplexityConfig.DEFAULT);
        lenient().when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(UserConfiguration.DEFAULT_VALUES);
        lenient().when(userContext.getUser()).thenReturn(user);
        lenient().when(user.getName()).thenReturn("username");
        lenient().when(user.getId()).thenReturn("userId");
    }

    /**
     * Verify user is successfully created in the Create flow.
     * This tests the integration between the UsersResource and UserManagementServiceImpl.
     */
    @Test
    void createSuccess() throws ValidationException, NotFoundException {
        Role role = mock(Role.class);
        when(role.getId()).thenReturn(new ObjectId().toHexString());
        when(role.getName()).thenReturn(TestUsersResource.ALLOWED_ROLE);

        when(roleService.loadAllLowercaseNameMap()).thenReturn(Map.of(ALLOWED_ROLE_LOWER_CASE, role));
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(UserConfiguration.DEFAULT_VALUES);
        when(userManagementService.create()).thenReturn(userImplFactory.create(new HashMap<>()));

        final var creator = userImplFactory.create(Map.of(UserImpl.USERNAME, "creator"));
        when(userService.loadById("creator")).thenReturn(creator);
        when(subject.getPrincipal()).thenReturn(creator.getName());

        final Response response = usersResource.create(buildCreateUserRequest(List.of(TestUsersResource.ALLOWED_ROLE), PASSWORD), userContext);
        Assertions.assertEquals(201, response.getStatus());
        verify(userManagementService).create(isA(UserImpl.class), eq(creator));
    }

    @Test
    void failOnUnallowedRoleAssignment() throws NotFoundException {
        Role readerRole = mock(Role.class);
        when(readerRole.getId()).thenReturn(new ObjectId().toHexString());
        when(readerRole.getName()).thenReturn(TestUsersResource.ALLOWED_ROLE);

        String forbiddenRole = "ADMIN";
        Role adminRole = mock(Role.class);
        when(adminRole.getId()).thenReturn(new ObjectId().toHexString());
        when(adminRole.getName()).thenReturn("admin");

        when(roleService.loadAllLowercaseNameMap()).thenReturn(Map.of(ALLOWED_ROLE_LOWER_CASE, readerRole, "admin", adminRole));
        when(userManagementService.create()).thenReturn(userImplFactory.create(new HashMap<>()));
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(UserConfiguration.DEFAULT_VALUES);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> usersResource.create(buildCreateUserRequest(List.of(TestUsersResource.ALLOWED_ROLE, forbiddenRole), PASSWORD), userContext));
        assertThat(exception.getMessage()).contains("Not authorized to access resource id <admin>");
    }

    @Test
    void failOnUnallowedRoleUnAssignment() throws NotFoundException {
        Role readerRole = mock(Role.class);
        ObjectId readerRoleId = new ObjectId();
        when(readerRole.getId()).thenReturn(readerRoleId.toHexString());
        when(readerRole.getName()).thenReturn(TestUsersResource.ALLOWED_ROLE);

        Role adminRole = mock(Role.class);
        ObjectId adminRoleId = new ObjectId();
        when(adminRole.getId()).thenReturn(adminRoleId.toHexString());
        when(adminRole.getName()).thenReturn("ADMIN");

        when(roleService.loadAllLowercaseNameMap()).thenReturn(Map.of(ALLOWED_ROLE_LOWER_CASE, readerRole, "admin", adminRole));

        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(UserConfiguration.DEFAULT_VALUES);
        when(userManagementService.create()).thenReturn(userImplFactory.create(Map.of(UserImpl.ROLES, List.of(readerRoleId, adminRoleId))));

        final var creator = userImplFactory.create(Map.of(UserImpl.USERNAME, "creator"));
        lenient().when(userService.loadById("creator")).thenReturn(creator);
        when(subject.getPrincipal()).thenReturn(creator.getName());

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> usersResource.create(buildCreateUserRequest(List.of(), PASSWORD), userContext));
        assertThat(exception.getMessage()).contains("Not authorized to access resource id <ADMIN>");
    }

    @Test
    void createFailureOnWrongRoleAssignPermission() {
        String testRole = "forbiddenRole";
        when(userManagementService.create()).thenReturn(userImplFactory.create(new HashMap<>()));
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(UserConfiguration.DEFAULT_VALUES);
        assertThrows(BadRequestException.class, () -> usersResource.create(buildCreateUserRequest(List.of(testRole), PASSWORD), userContext));
    }

    @Test
    void createFailurePasswordLength() {
        assertThrows(BadRequestException.class, () -> usersResource.create(buildCreateUserRequest(Collections.emptyList(), "pW1&"), userContext));
    }

    @Test
    void createFailurePasswordCasing() {
        when(clusterConfigService.getOrDefault(PasswordComplexityConfig.class, PasswordComplexityConfig.DEFAULT)).thenReturn(new PasswordComplexityConfig(
                6, true, false, false, false
        ));
        assertThrows(BadRequestException.class, () -> usersResource.create(buildCreateUserRequest(Collections.emptyList(), "lowercase1&"), userContext));
    }

    @Test
    void createFailurePasswordSpecialChars() {
        when(clusterConfigService.getOrDefault(PasswordComplexityConfig.class, PasswordComplexityConfig.DEFAULT)).thenReturn(new PasswordComplexityConfig(
                6, false, false, false, true
        ));
        assertThrows(BadRequestException.class, () -> usersResource.create(buildCreateUserRequest(Collections.emptyList(), "passWORD123"), userContext));
    }

    @Test
    void savePreferencesSuccess() throws ValidationException {
        when(subject.isPermitted(anyString())).thenReturn(true);
        when(userManagementService.load(USERNAME)).thenReturn(userImplFactory.create(new HashMap<>()));
        usersResource.savePreferences(USERNAME, UpdateUserPreferences.create(new HashMap<>()));
        verify(userManagementService).save(isA(UserImpl.class));
    }

    @Test
    void createTokenSucceeds() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME);
        final Token expected = createTokenAndPrepareMocks(userProps, true);

        try {
            final Token actual = usersResource.generateNewToken(USERNAME, UsersResourceTest.TOKEN_NAME, new UsersResource.GenerateTokenTTL(Optional.of(PeriodDuration.of(Duration.ofDays(30)))));
            assertEquals(expected, actual);
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verify(accessTokenService).create(USERNAME, UsersResourceTest.TOKEN_NAME, PeriodDuration.of(Duration.ofDays(30)));
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    void createTokenForInternalUserWithoutTTLSucceedsAndLoadsConfig() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME);
        final Token expected = createTokenAndPrepareMocks(userProps, true);

        try {
            final Token actual = usersResource.generateNewToken(USERNAME, UsersResourceTest.TOKEN_NAME, new UsersResource.GenerateTokenTTL(Optional.empty()));
            assertEquals(expected, actual);
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verify(accessTokenService).create(USERNAME, UsersResourceTest.TOKEN_NAME, PeriodDuration.of(Duration.ofDays(30)));
            verify(clusterConfigService).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    void createTokenFailsIfCreateNotAllowed() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME);
        createTokenAndPrepareMocks(userProps, false);

        try {
            final UsersResource.GenerateTokenTTL ttl = new UsersResource.GenerateTokenTTL(Optional.of(PeriodDuration.of(Duration.ofDays(30))));
            assertThrows(ForbiddenException.class, () -> usersResource.generateNewToken(USERNAME, TOKEN_NAME, ttl));
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    void createTokenSucceedsEvenWithNULLBody() {
        final Map<String, Object> userProps = Map.of(UserImpl.USERNAME, USERNAME, UserImpl.EXTERNAL_USER, "FALSE");
        final Token expected = createTokenAndPrepareMocks(userProps, true);

        try {
            final Token actual = usersResource.generateNewToken(USERNAME, TOKEN_NAME, null);
            assertEquals(expected, actual);
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verify(clusterConfigService).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
            verify(accessTokenService).create(USERNAME, TOKEN_NAME, PeriodDuration.of(Duration.ofDays(30)));
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    void adminCanCreateTokensForOtherUsers() {
        final String adminUserName = "admin";
        final Map<String, Object> owningUser = Map.of(UserImpl.USERNAME, USERNAME);
        final Map<String, Object> callingUser = Map.of(UserImpl.USERNAME, adminUserName);
        final Token expected = createTokenAndPrepareMocks(owningUser, callingUser, true);

        try {
            final Token actual = usersResource.generateNewToken(USERNAME, TOKEN_NAME, null);
            assertEquals(expected, actual);
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verify(clusterConfigService).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
            verify(accessTokenService).create(USERNAME, TOKEN_NAME, PeriodDuration.of(Duration.ofDays(30)));
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    void regularUserCannotCreateTokensForOtherUsers() {
        final String otherUserName = "Dee-Dee";
        final Map<String, Object> owningUser = Map.of(UserImpl.USERNAME, USERNAME);
        final Map<String, Object> callingUser = Map.of(UserImpl.USERNAME, otherUserName);
        createTokenAndPrepareMocks(owningUser, callingUser, false);

        try {
            assertThrows(ForbiddenException.class, () -> usersResource.generateNewToken(USERNAME, TOKEN_NAME, null));
        } finally {
            verify(subject).isPermitted(USERS_TOKENCREATE + ":" + USERNAME);
            verifyNoMoreInteractions(clusterConfigService, accessTokenService);
        }
    }

    @Test
    void getByIdUserWithAdditionalUserIdPermissions() {
        String alice = "alice";
        String bob = "bob";
        String unknown = "unknown";
        List<WildcardPermission> permissions = List.of(
                new WildcardPermission(RestPermissions.USERS_EDIT + ":" + alice),
                new WildcardPermission(RestPermissions.USERS_EDIT + ":" + bob),
                new WildcardPermission(RestPermissions.USERS_EDIT + ":" + unknown),
                new WildcardPermission("streams:read:12345"),
                new WildcardPermission("streams:read")
        );
        User userAlice = setupUser(alice, permissions);
        User userBob = userImplFactory.create(Map.of(UserImpl.USERNAME, bob));
        when(userManagementService.load(bob)).thenReturn(userBob);

        assertThat(usersResource.getbyId(userAlice.getId(), new UserContext(userAlice.getId(), subject, userService)).permissions().stream()
                .map(Object::toString).toList())
                .contains("users:edit:" + alice,
                        "users:edit:" + bob,
                        "users:edit:" + unknown,
                        "streams:read:12345",
                        "streams:read",
                        "users:edit:" + userAlice.getId(),
                        "users:edit:" + userBob.getId())
                .hasSize(permissions.size() + 2);
    }

    private User setupUser(String username, List<WildcardPermission> basePerms) {
        User user = userImplFactory.create(Map.of(UserImpl.USERNAME, username));
        String userId = user.getId();
        when(subject.isPermitted(RestPermissions.USERS_EDIT + ":" + username)).thenReturn(true);
        when(userManagementService.loadById(userId)).thenReturn(user);
        when(userService.loadById(userId)).thenReturn(user);
        when(userManagementService.getWildcardPermissionsForUser(user)).thenReturn(basePerms);
        when(userManagementService.getGRNPermissionsForUser(user)).thenReturn(List.of());
        return user;
    }

    private CreateUserRequest buildCreateUserRequest(List<String> roles, String password) {
        return CreateUserRequest.create(USERNAME, password, EMAIL,
                FIRST_NAME, LAST_NAME, Collections.singletonList(""),
                TIMEZONE, SESSION_TIMEOUT,
                new DashboardStartPage("dashboard-id"), roles, false);
    }

    private Token createTokenAndPrepareMocks(Map<String, Object> owningUser, Map<String, Object> callingUser, boolean isAdmin) {
        final String token = "someToken";
        final String callingUserName = (String) callingUser.get(UserImpl.USERNAME);
        final String owningUserName = (String) owningUser.get(UserImpl.USERNAME);
        final DateTime lastAccess = Tools.nowUTC();
        final Map<String, Object> tokenProps = Map.of(AccessTokenImpl.NAME, TOKEN_NAME, AccessTokenImpl.TOKEN, token, AccessTokenImpl.LAST_ACCESS, lastAccess);
        final ObjectId tokenId = new ObjectId();
        final AccessToken accessToken = new AccessTokenImpl(tokenId, tokenProps);
        final User user = userImplFactory.create(callingUser);
        if (isAdmin) {
            user.setRoleIds(Set.of(ADMIN_OBJECT_ID));
        }

        final boolean allowedToCreateToken = callingUserName.equals(owningUserName) || isAdmin;
        when(userManagementService.loadById(USERNAME)).thenReturn(userImplFactory.create(owningUser));
        when(subject.isPermitted(USERS_TOKENCREATE + ":" + owningUserName)).thenReturn(allowedToCreateToken);
        if (allowedToCreateToken) {
            when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES))
                    .thenReturn(UserConfiguration.create(false, Duration.of(8, ChronoUnit.HOURS), false, false, PeriodDuration.of(Duration.ofDays(30))));
            when(accessTokenService.create(USERNAME, UsersResourceTest.TOKEN_NAME, PeriodDuration.of(Duration.ofDays(30)))).thenReturn(accessToken);
        }

        return Token.create(tokenId.toHexString(), TOKEN_NAME, token, lastAccess);

    }

    private Token createTokenAndPrepareMocks(Map<String, Object> userProps, boolean allowCreateToken) {
        final String token = "someToken";
        final DateTime lastAccess = Tools.nowUTC();
        final Map<String, Object> tokenProps = Map.of(AccessTokenImpl.NAME, TOKEN_NAME, AccessTokenImpl.TOKEN, token, AccessTokenImpl.LAST_ACCESS, lastAccess);
        final ObjectId tokenId = new ObjectId();
        final AccessToken accessToken = allowCreateToken ? new AccessTokenImpl(tokenId, tokenProps) : null;

        prepareMocks(userProps, accessToken, allowCreateToken);

        return Token.create(tokenId.toHexString(), TOKEN_NAME, token, lastAccess);
    }

    private void prepareMocks(Map<String, Object> userProps, AccessToken accessToken, boolean allow) {
        final User user = userImplFactory.create(userProps);
        when(userManagementService.loadById(USERNAME)).thenReturn(user);
        when(subject.isPermitted(USERS_TOKENCREATE + ":" + USERNAME)).thenReturn(allow);
        if (allow) {
            lenient().when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(UserConfiguration.DEFAULT_VALUES);
        }
        if (accessToken != null) {
            when(accessTokenService.create(USERNAME, UsersResourceTest.TOKEN_NAME, PeriodDuration.of(Duration.ofDays(30)))).thenReturn(accessToken);
        }
    }

    /**
     * Test implementation of UsersResource is needed to set superclass configuration property
     * (which is directly injected without a constructor).
     */
    public static class TestUsersResource extends UsersResource {

        private static final String ALLOWED_ROLE = "READER";
        private final Subject subject;

        public TestUsersResource(UserManagementService userManagementService, PaginatedUserService paginatedUserService,
                                 AccessTokenService accessTokenService, RoleService roleService,
                                 SessionService sessionService, HttpConfiguration configuration,
                                 Subject subject, UserSessionTerminationService sessionTerminationService,
                                 DefaultSecurityManager securityManager, GlobalAuthServiceConfig globalAuthServiceConfig,
                                 ClusterConfigService clusterConfigService, UserService userService) {
            super(userManagementService, paginatedUserService, accessTokenService, roleService, sessionService,
                    sessionTerminationService, securityManager, globalAuthServiceConfig, clusterConfigService, mock(AuditEventSender.class));
            this.subject = subject;
            super.configuration = configuration;
            super.userService = userService;
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }

        @Override
        protected void checkPermission(String permission, String instanceId) {
            if (permission.equals(RestPermissions.ROLES_ASSIGN) && instanceId.equals(ALLOWED_ROLE)) {
                return;
            }
            super.checkPermission(permission, instanceId);
        }
    }

    public static class UserImplFactory implements UserImpl.Factory {
        private final Permissions permissions;
        private final PasswordAlgorithmFactory passwordAlgorithmFactory;
        private final ClusterConfigService configService;
        private final ObjectMapper objectMapper;

        public UserImplFactory(Permissions permissions, ClusterConfigService configService) {
            this.permissions = permissions;
            this.configService = configService;
            this.passwordAlgorithmFactory = new PasswordAlgorithmFactory(Collections.emptyMap(),
                    new SHA1HashPasswordAlgorithm("TESTSECRET"));
            this.objectMapper = new ObjectMapperProvider().get();
        }

        @Override
        public UserImpl create(Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, configService, objectMapper, fields);
        }

        @Override
        public UserImpl create(ObjectId id, Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, configService, objectMapper, id, fields);
        }

        // Not used.
        @Override
        public UserImpl.LocalAdminUser createLocalAdminUser(String adminRoleObjectId) {
            return null;
        }
    }
}
