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
import jakarta.ws.rs.ForbiddenException;
import org.bson.types.ObjectId;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQueryParser;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
class AuthzRolesResourceTest {
    @Mock
    private PaginatedAuthzRolesService authzolesService;
    @Mock
    private PaginatedUserService paginatedUserService;
    @Mock
    private UserService userService;
    @Mock
    private SearchQueryParser searchQueryParser;
    @Mock
    private SearchQueryParser userSearchQueryParser;
    @Mock
    AuditEventSender auditEventSender;
    @Mock
    private RoleService roleService;

    private AuthzRolesResource classUnderTest;

    @BeforeEach
    void setUp() {
        // A real PrivilegeEscalationGuard is used on purpose: these tests are about the permission semantics
        // the resource enforces. `validatePermissions` never touches the RoleService.
        classUnderTest = new AuthzRolesResource(authzolesService, paginatedUserService, userService,
                auditEventSender, new PrivilegeEscalationGuard(roleService));
    }

    @Test
    @WithAuthorization(permissions = "roles:read:reader")
    void testGetSingleRole() {
        ObjectId readerRoleId = new ObjectId();
        AuthzRoleDTO readerRole = mock(AuthzRoleDTO.class);
        String idHexString = readerRoleId.toHexString();

        when(authzolesService.get(eq(idHexString))).thenReturn(Optional.of(readerRole));
        when(readerRole.id()).thenReturn(idHexString);
        when(readerRole.name()).thenReturn("reader");

        AuthzRoleDTO authzRoleDTO = classUnderTest.get(idHexString);
        assertThat(authzRoleDTO.id()).isEqualTo(idHexString);
        assertThat(authzRoleDTO.name()).isEqualTo("reader");
    }

    @Test
    @WithAuthorization(permissions = "roles:read:random")
    void testGEtSignleRoleThrowsAccessError() {
        ObjectId readerRoleId = new ObjectId();
        AuthzRoleDTO readerRole = mock(AuthzRoleDTO.class);
        String idHexString = readerRoleId.toHexString();

        when(authzolesService.get(eq(idHexString))).thenReturn(Optional.of(readerRole));
        when(readerRole.name()).thenReturn("reader");
        assertThrows(ForbiddenException.class, () -> classUnderTest.get(idHexString));
    }

    @Test
    @WithAuthorization(permissions = { "users:rolesedit:johnny", "roles:assign:reader" })
    void testAddingUserToRole() throws ValidationException {
        ObjectId readerRoleId = new ObjectId();
        AuthzRoleDTO readerRole = mock(AuthzRoleDTO.class);
        String idHexString = readerRoleId.toHexString();
        UserContext userContext = mock(UserContext.class);

        when(authzolesService.get(eq(idHexString))).thenReturn(Optional.of(readerRole));
        when(readerRole.name()).thenReturn("reader");
        User user = mock(User.class);
        when(userService.load(eq("johnny"))).thenReturn(user);
        when(userContext.getUser()).thenReturn(user);
        when(user.getName()).thenReturn("johnny");

        classUnderTest.addUser(idHexString, Set.of("johnny"), userContext);

        verify(user).setRoleIds(eq(Set.of(idHexString)));
    }

    @Test
    @WithAuthorization(permissions = { "roles:assign:reader" })
    void testAddingUserToRoleFailsWithoutRolesEdit() {
        ObjectId readerRoleId = new ObjectId();
        AuthzRoleDTO readerRole = mock(AuthzRoleDTO.class);
        String idHexString = readerRoleId.toHexString();

        assertThrows(ForbiddenException.class, () -> classUnderTest.addUser(idHexString, Set.of("johnny"), mock(UserContext.class)));
    }

    @Test
    @WithAuthorization(permissions = { "users:rolesedit:johnny", "roles:assign:wrong" })
    void testAddingUserToRoleFailsWithWrongAssignRole() {
        ObjectId readerRoleId = new ObjectId();
        AuthzRoleDTO readerRole = mock(AuthzRoleDTO.class);
        String idHexString = readerRoleId.toHexString();

        when(authzolesService.get(eq(idHexString))).thenReturn(Optional.of(readerRole));
        when(readerRole.name()).thenReturn("reader");
        User user = mock(User.class);
        when(userService.load(eq("johnny"))).thenReturn(user);

        assertThrows(ForbiddenException.class, () -> classUnderTest.addUser(idHexString, Set.of("johnny"), mock(UserContext.class)));
    }

    // ----------------------------------------------------------------------------------------------------
    // A role granting more than the caller holds may not be assigned, even with `roles:assign` on it.
    // ----------------------------------------------------------------------------------------------------

    @Test
    @WithAuthorization(permissions = {"users:rolesedit:johnny", "roles:assign:admin", "streams:read"})
    void addingUserToRoleSucceedsWhenCurrentUserHoldsAllPermissionsGrantedByTheRole() {
        final String roleId = new ObjectId().toHexString();
        final AuthzRoleDTO adminRole = mock(AuthzRoleDTO.class);
        when(adminRole.name()).thenReturn("admin");
        when(adminRole.permissions()).thenReturn(Set.of("streams:read"));
        when(authzolesService.get(eq(roleId))).thenReturn(Optional.of(adminRole));

        final User user = mock(User.class);
        when(userService.load(eq("johnny"))).thenReturn(user);

        classUnderTest.addUser(roleId, Set.of("johnny"), SecurityTestUtils.getUserContext());

        verify(user).setRoleIds(eq(Set.of(roleId)));
    }

    @Test
    @WithAuthorization(permissions = {"users:rolesedit:johnny", "roles:assign:admin"})
    void addingUserToRoleFailsWhenRoleGrantsAPermissionCurrentUserLacks() throws ValidationException {
        // Holding `roles:assign` on the admin role is not enough to make somebody an admin.
        final String roleId = new ObjectId().toHexString();
        final AuthzRoleDTO adminRole = mock(AuthzRoleDTO.class);
        when(adminRole.name()).thenReturn("admin");
        when(adminRole.permissions()).thenReturn(Set.of("*"));
        when(authzolesService.get(eq(roleId))).thenReturn(Optional.of(adminRole));

        final User user = mock(User.class);
        when(userService.load(eq("johnny"))).thenReturn(user);

        assertThatThrownBy(() -> classUnderTest.addUser(roleId, Set.of("johnny"), SecurityTestUtils.getUserContext()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("*");

        verify(user, never()).setRoleIds(any());
        verify(userService, never()).save(any(User.class));
    }

    @Test
    @WithAuthorization(permissions = {"users:rolesedit:johnny", "roles:assign:admin", "streams:*"})
    void addingUserToRoleFailsOnASinglePermissionTheCurrentUserLacks() {
        final String roleId = new ObjectId().toHexString();
        final AuthzRoleDTO adminRole = mock(AuthzRoleDTO.class);
        when(adminRole.name()).thenReturn("admin");
        when(adminRole.permissions()).thenReturn(Set.of("streams:read", "inputs:create"));
        when(authzolesService.get(eq(roleId))).thenReturn(Optional.of(adminRole));

        final User user = mock(User.class);
        when(userService.load(eq("johnny"))).thenReturn(user);

        assertThatThrownBy(() -> classUnderTest.addUser(roleId, Set.of("johnny"), SecurityTestUtils.getUserContext()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inputs:create")
                .hasMessageNotContaining("streams:read");
    }

    @Test
    @WithAuthorization(permissions = {"users:rolesedit:johnny", "roles:assign:admin"})
    void removingUserFromRoleDoesNotRequireThePermissionsGrantedByTheRole() {
        // Un-assigning a role reduces privileges, so it must not be gated on holding the role's permissions -
        // otherwise an over-privileged user could never be demoted by a less privileged administrator.
        final String roleId = new ObjectId().toHexString();
        final AuthzRoleDTO adminRole = mock(AuthzRoleDTO.class);
        when(adminRole.name()).thenReturn("admin");
        when(authzolesService.get(eq(roleId))).thenReturn(Optional.of(adminRole));

        final User user = mock(User.class);
        when(userService.load(eq("johnny"))).thenReturn(user);

        assertThatCode(() -> classUnderTest.removeUser(roleId, "johnny", SecurityTestUtils.getUserContext()))
                .doesNotThrowAnyException();
    }
}
