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
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.Configuration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.users.requests.CreateUserRequest;
import org.graylog2.rest.models.users.requests.Startpage;
import org.graylog2.rest.models.users.requests.UpdateUserPreferences;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.MongoDBSessionService;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.security.hashing.SHA1HashPasswordAlgorithm;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserManagementService;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UsersResourceTest {

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "test@graylog.com";
    public static final String FIRST_NAME = "First";
    public static final String LAST_NAME = "Last";
    public static final String TIMEZONE = "Europe/Berlin";
    public static final long SESSION_TIMEOUT = 0L;

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

    UserImplFactory userImplFactory;

    @Before
    public void setUp() throws Exception {
        userImplFactory = new UserImplFactory(new Configuration(),
                                              new Permissions(ImmutableSet.of(new RestPermissions())));
        usersResource = new TestUsersResource(userManagementService, paginatedUserService, accessTokenService,
                                              roleService, sessionService, new HttpConfiguration(), subject);
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
        when(userManagementService.load(eq(USERNAME))).thenReturn(userImplFactory.create(new HashMap<>()));
        usersResource.savePreferences(USERNAME, UpdateUserPreferences.create(new HashMap<>()));
        verify(userManagementService, times(1)).save(isA(UserImpl.class));
    }

    private CreateUserRequest buildCreateUserRequest() {
        return CreateUserRequest.create(USERNAME, PASSWORD, EMAIL,
                                        FIRST_NAME, LAST_NAME, Collections.singletonList(""),
                                        TIMEZONE, SESSION_TIMEOUT,
                                        startPage, Collections.emptyList());
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
                                 Subject subject) {
            super(userManagementService, paginatedUserService, accessTokenService, roleService, sessionService);
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
            return new UserImpl(passwordAlgorithmFactory, permissions, fields);
        }

        @Override
        public UserImpl create(ObjectId id, Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, id, fields);
        }

        // Not used.
        @Override
        public UserImpl.LocalAdminUser createLocalAdminUser(String adminRoleObjectId) {
            return null;
        }
    }
}
