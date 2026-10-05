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
package org.graylog2.rest.resources.roles;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.roles.responses.RoleResponse;
import org.graylog2.rest.models.roles.responses.RolesResponse;
import org.graylog2.security.SecurityTestUtils;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.users.Role;
import org.graylog2.users.PrivilegeEscalationGuard;
import org.graylog2.users.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
class RolesResourceTest {

    private static final String STREAM_READER_ROLE = "stream-reader";
    private static final String STREAM_READER_ROLE_ID = "stream-reader-id";
    private static final String ADMIN_ROLE = "admin";
    private static final String ADMIN_ROLE_ID = "admin-id";

    // The username the @WithAuthorization security context is built for. The UserService mocked by
    // SecurityTestUtils only resolves this user, so membership tests operate on the calling user - which is
    // also the interesting case, since assigning yourself a privileged role is the escalation we block.
    private static final String CURRENT_USER = "test_user";

    private static final String STREAMS_READ = "streams:read";
    private static final String INPUTS_CREATE = "inputs:create";

    @Mock
    private RoleService roleService;
    @Mock
    private GlobalAuthServiceConfig globalAuthServiceConfig;

    private TestRolesResource classUnderTest;

    @BeforeEach
    void setUp() {
        // A real PrivilegeEscalationGuard is used on purpose: these tests are about the permission semantics the
        // resource enforces, so they should exercise the actual Shiro wildcard resolution rather than assert
        // that a collaborator was called. `validatePermissions` never touches the RoleService.
        classUnderTest = new TestRolesResource(roleService, globalAuthServiceConfig,
                new PrivilegeEscalationGuard(roleService), new HttpConfiguration());
    }

    @Test
    @WithAuthorization(permissions = {"roles:read:reader"})
    void testListAll() {
        Role readerRole = mock(Role.class);
        Role adminRole = mock(Role.class);

        when(readerRole.getName()).thenReturn("reader");
        when(adminRole.getName()).thenReturn("admin");
        when(roleService.loadAll()).thenReturn(Set.of(readerRole, adminRole));

        RolesResponse rolesResponse = classUnderTest.listAll();

        Set<String> roleNames = rolesResponse.roles().stream()
                .map(r -> r.name())
                .collect(Collectors.toSet());

        assertEquals(Set.of("reader"), roleNames);
    }

    // ----------------------------------------------------------------------------------------------------
    // POST /roles - a role may not be created with permissions the creating user does not hold
    // ----------------------------------------------------------------------------------------------------

    @Test
    @WithAuthorization(permissions = {"roles:create", STREAMS_READ})
    void createRoleSucceedsWhenCurrentUserHoldsAllRequestedPermissions() throws ValidationException {
        givenSaveReturnsRole(STREAM_READER_ROLE, Set.of(STREAMS_READ));

        final Response response = classUnderTest.create(
                roleRequest(STREAM_READER_ROLE, Set.of(STREAMS_READ)), SecurityTestUtils.getUserContext());

        assertThat(response.getStatus()).isEqualTo(201);
        verify(roleService).save(any(Role.class));
    }

    @Test
    @WithAuthorization(permissions = {"roles:create", STREAMS_READ})
    void createRoleSucceedsWithoutAnyPermissions() throws ValidationException {
        givenSaveReturnsRole(STREAM_READER_ROLE, Set.of());

        final Response response = classUnderTest.create(
                roleRequest(STREAM_READER_ROLE, Set.of()), SecurityTestUtils.getUserContext());

        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    @WithAuthorization(permissions = {"roles:create"})
    void createRoleFailsWhenCurrentUserLacksARequestedPermission() throws ValidationException {
        final var request = roleRequest(STREAM_READER_ROLE, Set.of(INPUTS_CREATE));
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.create(request, userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(INPUTS_CREATE);

        verify(roleService, never()).save(any(Role.class));
    }

    @Test
    @WithAuthorization(permissions = {"roles:create"})
    void createRoleFailsWhenGrantingTheWildcardPermission() throws ValidationException {
        final var request = roleRequest(ADMIN_ROLE, Set.of("*"));
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.create(request, userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("*");

        verify(roleService, never()).save(any(Role.class));
    }

    @Test
    @WithAuthorization(permissions = {"roles:create", "streams:*"})
    void createRoleAcceptsPermissionImpliedByAWildcardTheCurrentUserHolds() throws ValidationException {
        givenSaveReturnsRole(STREAM_READER_ROLE, Set.of(STREAMS_READ));

        final Response response = classUnderTest.create(
                roleRequest(STREAM_READER_ROLE, Set.of(STREAMS_READ)), SecurityTestUtils.getUserContext());

        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    @WithAuthorization(permissions = {"roles:create", "streams:read:12345"})
    void createRoleRejectsPermissionBroaderThanTheOneCurrentUserHolds() throws ValidationException {
        // Holding `streams:read:12345` must not allow granting `streams:read` for *all* streams.
        final var request = roleRequest(STREAM_READER_ROLE, Set.of(STREAMS_READ));
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.create(request, userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(STREAMS_READ);

        verify(roleService, never()).save(any(Role.class));
    }

    @Test
    @WithAuthorization(permissions = {STREAMS_READ})
    void createRoleStillRequiresTheRolesCreatePermission() {
        final var request = roleRequest(STREAM_READER_ROLE, Set.of(STREAMS_READ));
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.create(request, userContext))
                .isInstanceOf(ForbiddenException.class);
    }

    // ----------------------------------------------------------------------------------------------------
    // PUT /roles/{rolename} - a role may not be given permissions the editing user does not hold
    // ----------------------------------------------------------------------------------------------------

    @Test
    @WithAuthorization(permissions = {"roles:edit:" + STREAM_READER_ROLE, STREAMS_READ})
    void updateRoleSucceedsWhenCurrentUserHoldsAllRequestedPermissions()
            throws NotFoundException, ValidationException {
        final Role existingRole = mock(Role.class);
        when(existingRole.getName()).thenReturn(STREAM_READER_ROLE);
        when(existingRole.getPermissions()).thenReturn(Set.of(STREAMS_READ));
        when(roleService.load(STREAM_READER_ROLE)).thenReturn(existingRole);

        final RoleResponse result = classUnderTest.update(STREAM_READER_ROLE,
                roleRequest(STREAM_READER_ROLE, Set.of(STREAMS_READ)), SecurityTestUtils.getUserContext());

        assertThat(result.permissions()).containsExactly(STREAMS_READ);
        verify(existingRole).setPermissions(Set.of(STREAMS_READ));
        verify(roleService).save(existingRole);
    }

    @Test
    @WithAuthorization(permissions = {"roles:edit:" + ADMIN_ROLE})
    void updateRoleFailsWhenAddingAPermissionCurrentUserLacks() throws ValidationException {
        // A member of a role with `roles:edit` on it must not be able to escalate by widening that role.
        final var request = roleRequest(ADMIN_ROLE, Set.of("*"));
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.update(ADMIN_ROLE, request, userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("*");

        verify(roleService, never()).save(any(Role.class));
    }

    @Test
    @WithAuthorization(permissions = {"roles:edit:" + ADMIN_ROLE})
    void updateRoleValidatesBeforeLoadingTheRole() throws NotFoundException, ValidationException {
        final var request = roleRequest(ADMIN_ROLE, Set.of(INPUTS_CREATE));
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.update(ADMIN_ROLE, request, userContext))
                .isInstanceOf(BadRequestException.class);

        // The rejection happens before the role is even read from the database.
        verify(roleService, never()).load(any(String.class));
        verify(roleService, never()).save(any(Role.class));
    }

    @Test
    @WithAuthorization(permissions = {STREAMS_READ})
    void updateRoleStillRequiresTheRolesEditPermission() {
        final var request = roleRequest(STREAM_READER_ROLE, Set.of(STREAMS_READ));
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.update(STREAM_READER_ROLE, request, userContext))
                .isInstanceOf(ForbiddenException.class);
    }

    // ----------------------------------------------------------------------------------------------------
    // PUT /roles/{rolename}/members/{username} - a role granting more than the caller has may not be assigned
    // ----------------------------------------------------------------------------------------------------

    @Test
    @WithAuthorization(permissions = {
            "users:edit:" + CURRENT_USER, "roles:assign:" + STREAM_READER_ROLE, STREAMS_READ})
    void addMemberSucceedsWhenCurrentUserHoldsAllPermissionsGrantedByTheRole() throws NotFoundException {
        final Role role = mock(Role.class);
        when(role.getId()).thenReturn(STREAM_READER_ROLE_ID);
        when(role.getPermissions()).thenReturn(Set.of(STREAMS_READ));
        when(roleService.load(STREAM_READER_ROLE)).thenReturn(role);

        final Response response = classUnderTest.addMember(
                STREAM_READER_ROLE, CURRENT_USER, "{}", SecurityTestUtils.getUserContext());

        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    @WithAuthorization(permissions = {"users:edit:" + CURRENT_USER, "roles:assign:" + ADMIN_ROLE})
    void addMemberFailsWhenRoleGrantsAPermissionCurrentUserLacks() throws NotFoundException {
        // Holding `roles:assign` on the admin role is no longer enough to make yourself an admin.
        final Role adminRole = mock(Role.class);
        when(adminRole.getPermissions()).thenReturn(Set.of("*"));
        when(roleService.load(ADMIN_ROLE)).thenReturn(adminRole);

        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.addMember(ADMIN_ROLE, CURRENT_USER, "{}", userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("*");
    }

    @Test
    @WithAuthorization(permissions = {
            "users:edit:" + CURRENT_USER, "roles:assign:" + ADMIN_ROLE, "streams:*"})
    void addMemberFailsOnASinglePermissionTheCurrentUserLacks() throws NotFoundException {
        final Role adminRole = mock(Role.class);
        when(adminRole.getPermissions()).thenReturn(Set.of(STREAMS_READ, INPUTS_CREATE));
        when(roleService.load(ADMIN_ROLE)).thenReturn(adminRole);

        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.addMember(ADMIN_ROLE, CURRENT_USER, "{}", userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(INPUTS_CREATE)
                .hasMessageNotContaining(STREAMS_READ);
    }

    @Test
    @WithAuthorization(permissions = {"roles:assign:" + STREAM_READER_ROLE, STREAMS_READ})
    void addMemberStillRequiresTheUsersEditPermission() {
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.addMember(
                STREAM_READER_ROLE, CURRENT_USER, "{}", userContext))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @WithAuthorization(permissions = {"users:edit:" + CURRENT_USER, STREAMS_READ})
    void addMemberStillRequiresTheRolesAssignPermission() {
        final var userContext = SecurityTestUtils.getUserContext();

        assertThatThrownBy(() -> classUnderTest.addMember(
                STREAM_READER_ROLE, CURRENT_USER, "{}", userContext))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @WithAuthorization(permissions = {"users:edit:" + CURRENT_USER, "roles:assign:" + ADMIN_ROLE})
    void removeMemberDoesNotRequireThePermissionsGrantedByTheRole() throws NotFoundException {
        // Un-assigning a role reduces privileges, so it must not be gated on holding the role's permissions -
        // otherwise an over-privileged user could never be demoted by a less privileged administrator.
        final Role adminRole = mock(Role.class);
        when(adminRole.getId()).thenReturn(ADMIN_ROLE_ID);
        when(roleService.load(ADMIN_ROLE)).thenReturn(adminRole);

        assertThatCode(() -> classUnderTest.removeMember(ADMIN_ROLE, CURRENT_USER))
                .doesNotThrowAnyException();
    }

    // ----------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------

    private RoleResponse roleRequest(String name, Set<String> permissions) {
        return RoleResponse.create(name, Optional.empty(), permissions, false);
    }

    private void givenSaveReturnsRole(String name, Set<String> permissions) throws ValidationException {
        final Role saved = mock(Role.class);
        when(saved.getName()).thenReturn(name);
        when(saved.getPermissions()).thenReturn(permissions);
        when(roleService.save(any(Role.class))).thenReturn(saved);
    }

    /**
     * Test implementation of RolesResource is needed to set the superclass configuration property
     * (which is directly injected without a constructor) required for building the "created" location URI.
     */
    static class TestRolesResource extends RolesResource {
        TestRolesResource(RoleService roleService,
                          GlobalAuthServiceConfig globalAuthServiceConfig,
                          PrivilegeEscalationGuard privilegeEscalationGuard,
                          HttpConfiguration configuration) {
            super(roleService, globalAuthServiceConfig, privilegeEscalationGuard);
            super.configuration = configuration;
        }
    }
}
