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
package org.graylog.security.authzroles;

import jakarta.ws.rs.BadRequestException;
import org.bson.types.ObjectId;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.SecurityTestUtils;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.PrivilegeEscalationGuard;
import org.graylog2.users.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
class AuthzRolesResourceTest {

    // On this branch the membership endpoints are gated on `roles:edit` - the separate `roles:assign`
    // permission is a later change that is not part of this security fix.
    private static final String TARGET_USER = "johnny";

    @Mock
    private PaginatedAuthzRolesService authzRolesService;
    @Mock
    private PaginatedUserService paginatedUserService;
    @Mock
    private UserService userService;
    @Mock
    private RoleService roleService;

    private AuthzRolesResource classUnderTest;

    @BeforeEach
    void setUp() {
        // A real PrivilegeEscalationGuard is used on purpose: these tests are about the permission semantics
        // the resource enforces. `validatePermissions` never touches the RoleService.
        classUnderTest = new AuthzRolesResource(authzRolesService, paginatedUserService, userService,
                new PrivilegeEscalationGuard(roleService));
    }

    @Test
    @WithAuthorization(permissions = {"users:rolesedit:johnny", "roles:edit:admin", "streams:read"})
    void addingUserToRoleSucceedsWhenCurrentUserHoldsAllPermissionsGrantedByTheRole() throws Exception {
        final String roleId = givenRole("admin", Set.of("streams:read"));
        final User user = givenTargetUser();

        classUnderTest.addUser(roleId, Set.of(TARGET_USER), SecurityTestUtils.getUserContext(userService));

        verify(user).setRoleIds(eq(Set.of(roleId)));
    }

    @Test
    @WithAuthorization(permissions = {"users:rolesedit:johnny", "roles:edit:admin"})
    void addingUserToRoleFailsWhenRoleGrantsAPermissionCurrentUserLacks() throws Exception {
        // Holding `roles:edit` on the admin role is not enough to make somebody an admin.
        final String roleId = givenRole("admin", Set.of("*"));
        final User user = givenTargetUser();

        assertThatThrownBy(() -> classUnderTest.addUser(
                roleId, Set.of(TARGET_USER), SecurityTestUtils.getUserContext(userService)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("*");

        verify(user, never()).setRoleIds(any());
        verify(userService, never()).save(any(User.class));
    }

    @Test
    @WithAuthorization(permissions = {"users:rolesedit:johnny", "roles:edit:admin", "streams:*"})
    void addingUserToRoleFailsOnASinglePermissionTheCurrentUserLacks() throws Exception {
        final String roleId = givenRole("admin", Set.of("streams:read", "inputs:create"));
        givenTargetUser();

        assertThatThrownBy(() -> classUnderTest.addUser(
                roleId, Set.of(TARGET_USER), SecurityTestUtils.getUserContext(userService)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inputs:create")
                .hasMessageNotContaining("streams:read");
    }

    @Test
    @WithAuthorization(permissions = {"users:rolesedit:johnny", "roles:edit:admin"})
    void removingUserFromRoleDoesNotRequireThePermissionsGrantedByTheRole() throws Exception {
        // Un-assigning a role reduces privileges, so it must not be gated on holding the role's permissions -
        // otherwise an over-privileged user could never be demoted by a less privileged administrator.
        final String roleId = givenRole("admin", null);
        givenTargetUser();

        assertThatCode(() -> classUnderTest.removeUser(
                roleId, TARGET_USER, SecurityTestUtils.getUserContext(userService)))
                .doesNotThrowAnyException();
    }

    private String givenRole(String name, Set<String> permissions) {
        final String roleId = new ObjectId().toHexString();
        final AuthzRoleDTO role = mock(AuthzRoleDTO.class);
        when(role.name()).thenReturn(name);
        if (permissions != null) {
            when(role.permissions()).thenReturn(permissions);
        }
        when(authzRolesService.get(eq(roleId))).thenReturn(Optional.of(role));
        return roleId;
    }

    private User givenTargetUser() {
        final User user = mock(User.class);
        when(userService.load(eq(TARGET_USER))).thenReturn(user);
        return user;
    }
}
