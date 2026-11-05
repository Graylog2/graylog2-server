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
package org.graylog.security.shares;

import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.GranteeAuthorizer;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultGranteeServiceTest {
    private static final String VIEWER_ID = "54e3deadbeefdeadbeef0001";
    private static final String JANE_ID = "54e3deadbeefdeadbeef0002";
    private static final String JOHN_ID = "54e3deadbeefdeadbeef0003";
    private static final String DELETED_ID = "54e3deadbeefdeadbeef0009";

    private final GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    @Mock
    private UserService userService;
    @Mock
    private GranteeAuthorizer.Factory granteeAuthorizerFactory;
    @Mock
    private GranteeAuthorizer authorizer;
    @Mock
    private ClusterConfigService clusterConfigService;

    private DefaultGranteeService granteeService;
    private User viewer;
    private Map<String, User> users;

    @BeforeEach
    void setUp() {
        viewer = mockUser(VIEWER_ID, "viewer", "The Viewer");
        users = Map.of(
                VIEWER_ID, viewer,
                JANE_ID, mockUser(JANE_ID, "jane", "Jane Doe"),
                JOHN_ID, mockUser(JOHN_ID, "john", "John Doe"));

        lenient().when(granteeAuthorizerFactory.create(viewer)).thenReturn(authorizer);
        lenient().when(userService.loadByIds(anyCollection())).thenAnswer(invocation -> {
            final Collection<String> ids = invocation.getArgument(0);
            return ids.stream().filter(users::containsKey).map(users::get).toList();
        });

        granteeService = new DefaultGranteeService(userService, grnRegistry, granteeAuthorizerFactory, clusterConfigService);
    }

    @Test
    void keepsAllUsersWithListPermission() {
        when(authorizer.isPermitted(RestPermissions.USERS_LIST)).thenReturn(true);

        assertThat(granteeService.getVisibleGrantees(viewer, Set.of(user(JANE_ID), user(JOHN_ID)))).containsExactlyInAnyOrder(
                Grantee.createUser(user(JANE_ID), "Jane Doe"),
                Grantee.createUser(user(JOHN_ID), "John Doe"));
    }

    @Test
    void keepsUsersWithReadPermission() {
        when(authorizer.isPermitted(RestPermissions.USERS_LIST)).thenReturn(false);
        when(authorizer.isPermitted(eq(RestPermissions.USERS_READ), anyString())).thenAnswer(invocation -> "jane".equals(invocation.getArgument(1)));

        assertThat(granteeService.getVisibleGrantees(viewer, Set.of(user(JANE_ID), user(JOHN_ID)))).containsExactly(
                Grantee.createUser(user(JANE_ID), "Jane Doe"));
    }

    @Test
    void leavesOutViewer() {
        lenient().when(authorizer.isPermitted(RestPermissions.USERS_LIST)).thenReturn(true);

        assertThat(granteeService.getVisibleGrantees(viewer, Set.of(user(VIEWER_ID)))).isEmpty();
    }

    @Test
    void dropsDeletedUsers() {
        when(authorizer.isPermitted(RestPermissions.USERS_LIST)).thenReturn(true);

        assertThat(granteeService.getVisibleGrantees(viewer, Set.of(user(DELETED_ID), user(JANE_ID)))).containsExactly(
                Grantee.createUser(user(JANE_ID), "Jane Doe"));
    }

    @Test
    void dropsLocalAdminAndInvalidUserIds() {
        assertThat(granteeService.getVisibleGrantees(viewer, Set.of(user("local:admin"), user("not-an-object-id")))).isEmpty();
        verify(userService, never()).loadByIds(any());
    }

    @Test
    void keepsEveryoneAndDropsOtherTypes() {
        final GRN stream = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeef0004");

        assertThat(granteeService.getVisibleGrantees(viewer, Set.of(GRNRegistry.GLOBAL_USER_GRN, stream))).containsExactly(
                Grantee.createGlobal());
    }

    @Test
    void neverLoadsAllUsers() {
        when(authorizer.isPermitted(RestPermissions.USERS_LIST)).thenReturn(false);
        when(authorizer.isPermitted(eq(RestPermissions.USERS_READ), anyString())).thenReturn(false);

        granteeService.getVisibleGrantees(viewer, Set.of(user(JANE_ID), GRNRegistry.GLOBAL_USER_GRN));

        verify(userService).loadByIds(Set.of(JANE_ID));
        verify(userService, never()).loadAll();
    }

    private GRN user(String id) {
        return grnRegistry.newGRN(GRNTypes.USER, id);
    }

    private static User mockUser(String id, String name, String fullName) {
        final User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getName()).thenReturn(name);
        lenient().when(user.getFullName()).thenReturn(fullName);
        return user;
    }
}
