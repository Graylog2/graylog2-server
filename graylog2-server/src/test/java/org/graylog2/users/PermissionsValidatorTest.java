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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.graylog.security.UserContext;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.users.requests.CreateUserRequest;
import org.graylog2.shared.users.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.shared.security.RestPermissions.INPUTS_CREATE;
import static org.graylog2.shared.security.RestPermissions.ROLES_READ;
import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

    private static final String MISSING_PERMISSIONS_PREFIX =
            "Cannot assign permissions/roles to new user that current user does not have: ";
    private static final String DASHBOARDS_CREATE = "dashboards:create";

    // Role names are deliberately spelled in mixed/upper case, because the validator is expected to normalize them.
    private static final String READER_ROLE = "Reader";
    private static final String ADMIN_ROLE = "ADMIN";

    @Mock
    private RoleService roleService;
    @Mock
    private UserContext userContext;

    private PermissionsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PermissionsValidator(roleService);
    }

    // ----------------------------------------------------------------------------------------------------
    // validatePermissions - the plain "does the current user hold these permissions?" check
    // ----------------------------------------------------------------------------------------------------

    @Test
    void validatePermissionsPassesWhenCurrentUserHoldsAllOfThem() {
        currentUserHolds(STREAMS_READ, INPUTS_CREATE);

        assertThatCode(() -> validator.validatePermissions(List.of(STREAMS_READ, INPUTS_CREATE), userContext))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePermissionsRejectsPermissionTheCurrentUserMisses() {
        currentUserHolds(STREAMS_READ);

        assertThatThrownBy(() -> validator.validatePermissions(List.of(STREAMS_READ, INPUTS_CREATE), userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(MISSING_PERMISSIONS_PREFIX + INPUTS_CREATE);
    }

    @Test
    void validatePermissionsReportsEveryMissingPermissionAndNoHeldOnes() {
        currentUserHolds(STREAMS_READ);

        assertThatThrownBy(() -> validator.validatePermissions(
                List.of(STREAMS_READ, INPUTS_CREATE, DASHBOARDS_CREATE), userContext))
                .isInstanceOf(BadRequestException.class)
                // The missing permissions are collected into an unordered Set, so only membership is guaranteed.
                .hasMessageContaining(INPUTS_CREATE)
                .hasMessageContaining(DASHBOARDS_CREATE)
                .hasMessageNotContaining(STREAMS_READ);
    }

    @Test
    void validatePermissionsMentionsARepeatedMissingPermissionOnlyOnce() {
        assertThatThrownBy(() -> validator.validatePermissions(List.of(INPUTS_CREATE, INPUTS_CREATE), userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(MISSING_PERMISSIONS_PREFIX + INPUTS_CREATE);
    }

    @Test
    void validatePermissionsAcceptsEmptyCollection() {
        assertThatCode(() -> validator.validatePermissions(List.of(), userContext)).doesNotThrowAnyException();

        verifyNoInteractions(userContext);
    }

    @Test
    void validatePermissionsAcceptsNullCollection() {
        // `ChangeUserRequest.permissions()` is @Nullable, so the validator must cope with a missing list.
        assertThatCode(() -> validator.validatePermissions(null, userContext)).doesNotThrowAnyException();

        verifyNoInteractions(userContext);
    }

    @Test
    void validatePermissionsChecksPermissionStringsVerbatim() {
        // Graylog resolves user permissions case-sensitively (CaseSensitiveWildcardPermission), so unlike role
        // names, permission strings must be passed through untouched.
        final String mixedCasePermission = "Streams:Read:ABC123";
        currentUserHolds(mixedCasePermission);

        assertThatCode(() -> validator.validatePermissions(List.of(mixedCasePermission), userContext))
                .doesNotThrowAnyException();

        verify(userContext).isPermitted(mixedCasePermission);
    }

    // ----------------------------------------------------------------------------------------------------
    // validateRolePermissions - expands the requested roles and checks the permissions they grant
    // ----------------------------------------------------------------------------------------------------

    @Test
    void validateRolePermissionsPassesWhenCurrentUserHoldsEveryPermissionGrantedByTheRoles() throws NotFoundException {
        final Role readerRole = roleGranting(STREAMS_READ);
        allowReadingRoles(READER_ROLE);
        when(roleService.loadByNames(List.of(READER_ROLE))).thenReturn(Set.of(readerRole));
        currentUserHolds(STREAMS_READ);

        assertThatCode(() -> validator.validateRolePermissions(List.of(READER_ROLE), userContext))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRolePermissionsRejectsPermissionGrantedByRoleThatCurrentUserMisses() throws NotFoundException {
        final Role readerRole = roleGranting(INPUTS_CREATE);
        allowReadingRoles(READER_ROLE);
        when(roleService.loadByNames(List.of(READER_ROLE))).thenReturn(Set.of(readerRole));

        assertThatThrownBy(() -> validator.validateRolePermissions(List.of(READER_ROLE), userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(MISSING_PERMISSIONS_PREFIX + INPUTS_CREATE);
    }

    @Test
    void validateRolePermissionsAggregatesPermissionsAcrossAllRequestedRoles() throws NotFoundException {
        final Role readerRole = roleGranting(STREAMS_READ);
        final Role adminRole = roleGranting(INPUTS_CREATE);
        allowReadingRoles(READER_ROLE, ADMIN_ROLE);
        when(roleService.loadByNames(List.of(READER_ROLE, ADMIN_ROLE)))
                .thenReturn(Set.of(readerRole, adminRole));
        currentUserHolds(STREAMS_READ);

        assertThatThrownBy(() -> validator.validateRolePermissions(List.of(READER_ROLE, ADMIN_ROLE), userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(INPUTS_CREATE)
                .hasMessageNotContaining(STREAMS_READ);
    }

    @Test
    void validateRolePermissionsPassesForRolesThatGrantNothing() throws NotFoundException {
        final Role emptyRole = roleGranting();
        allowReadingRoles(READER_ROLE);
        when(roleService.loadByNames(List.of(READER_ROLE))).thenReturn(Set.of(emptyRole));

        assertThatCode(() -> validator.validateRolePermissions(List.of(READER_ROLE), userContext))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRolePermissionsLooksUpRolesByTheirLowercasedName() throws NotFoundException {
        allowReadingRoles(READER_ROLE, ADMIN_ROLE);
        when(roleService.loadByNames(any())).thenReturn(Set.of());

        validator.validateRolePermissions(List.of(READER_ROLE, ADMIN_ROLE), userContext);

        verify(roleService).loadByNames(List.of(READER_ROLE, ADMIN_ROLE));
    }

    @Test
    void validateRolePermissionsChecksReadAccessUsingTheLowercasedRoleName() throws NotFoundException {
        allowReadingRoles(ADMIN_ROLE);
        when(roleService.loadByNames(List.of(ADMIN_ROLE))).thenReturn(Set.of());

        validator.validateRolePermissions(List.of(ADMIN_ROLE), userContext);

        verify(userContext).isPermitted(ROLES_READ, ADMIN_ROLE);
    }

    @Test
    void validateRolePermissionsDeniesRolesTheCurrentUserCannotRead() {
        allowReadingRoles(READER_ROLE);

        assertThatThrownBy(() -> validator.validateRolePermissions(List.of(READER_ROLE, ADMIN_ROLE), userContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not allowed to read roles: " + ADMIN_ROLE);
    }

    @Test
    void validateRolePermissionsListsEveryUnreadableRole() {
        assertThatThrownBy(() -> validator.validateRolePermissions(List.of(ADMIN_ROLE, READER_ROLE), userContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not allowed to read roles: " + ADMIN_ROLE + ", " + READER_ROLE);
    }

    @Test
    void validateRolePermissionsDoesNotLoadRolesWhenReadAccessIsDenied() {
        assertThatThrownBy(() -> validator.validateRolePermissions(List.of(ADMIN_ROLE), userContext))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(roleService);
    }

    @Test
    void validateRolePermissionsAcceptsNullRoleList() throws NotFoundException {
        // `ChangeUserRequest.roles()` is @Nullable, so a request that touches no roles must not fail.
        when(roleService.loadByNames(List.of())).thenReturn(Set.of());

        assertThatCode(() -> validator.validateRolePermissions(null, userContext)).doesNotThrowAnyException();
    }

    @Test
    void validateRolePermissionsAcceptsEmptyRoleList() throws NotFoundException {
        when(roleService.loadByNames(List.of())).thenReturn(Set.of());

        assertThatCode(() -> validator.validateRolePermissions(List.of(), userContext)).doesNotThrowAnyException();
    }

    @Test
    void validateRolePermissionsTranslatesUnknownRolesIntoBadRequest() throws NotFoundException {
        final NotFoundException notFound = new NotFoundException("Couldn't find roles");
        allowReadingRoles(ADMIN_ROLE);
        when(roleService.loadByNames(List.of(ADMIN_ROLE))).thenThrow(notFound);

        assertThatThrownBy(() -> validator.validateRolePermissions(List.of(ADMIN_ROLE), userContext))
                .isInstanceOf(BadRequestException.class)
                .hasCause(notFound);
    }

    // ----------------------------------------------------------------------------------------------------
    // validatePermissionsAndRoles - the combined check used when creating a user
    // ----------------------------------------------------------------------------------------------------

    @Test
    void validatePermissionsAndRolesPassesWhenCurrentUserHoldsRoleAndDirectPermissions() throws NotFoundException {
        final Role readerRole = roleGranting(STREAMS_READ);
        allowReadingRoles(READER_ROLE);
        when(roleService.loadByNames(List.of(READER_ROLE))).thenReturn(Set.of(readerRole));
        currentUserHolds(STREAMS_READ, DASHBOARDS_CREATE);

        final var request = createUserRequest(List.of(READER_ROLE), List.of(DASHBOARDS_CREATE));

        assertThatCode(() -> validator.validatePermissionsAndRoles(request, userContext)).doesNotThrowAnyException();
    }

    @Test
    void validatePermissionsAndRolesRejectsDirectPermissionTheCurrentUserMisses() throws NotFoundException {
        final Role readerRole = roleGranting(STREAMS_READ);
        allowReadingRoles(READER_ROLE);
        when(roleService.loadByNames(List.of(READER_ROLE))).thenReturn(Set.of(readerRole));
        currentUserHolds(STREAMS_READ);

        final var request = createUserRequest(List.of(READER_ROLE), List.of(INPUTS_CREATE));

        assertThatThrownBy(() -> validator.validatePermissionsAndRoles(request, userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(MISSING_PERMISSIONS_PREFIX + INPUTS_CREATE);
    }

    @Test
    void validatePermissionsAndRolesRejectsPermissionLaunderedThroughARole() throws NotFoundException {
        // The role is readable, but it grants the wildcard permission, which the creating user does not hold.
        // This is the escalation the validator exists to prevent.
        final Role adminRole = roleGranting("*");
        allowReadingRoles(ADMIN_ROLE);
        when(roleService.loadByNames(List.of(ADMIN_ROLE))).thenReturn(Set.of(adminRole));

        final var request = createUserRequest(List.of(ADMIN_ROLE), List.of());

        assertThatThrownBy(() -> validator.validatePermissionsAndRoles(request, userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(MISSING_PERMISSIONS_PREFIX + "*");
    }

    @Test
    void validatePermissionsAndRolesReportsMissingRoleAndDirectPermissionsTogether() throws NotFoundException {
        final Role adminRole = roleGranting(INPUTS_CREATE);
        allowReadingRoles(ADMIN_ROLE);
        when(roleService.loadByNames(List.of(ADMIN_ROLE))).thenReturn(Set.of(adminRole));

        final var request = createUserRequest(List.of(ADMIN_ROLE), List.of(DASHBOARDS_CREATE));

        assertThatThrownBy(() -> validator.validatePermissionsAndRoles(request, userContext))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(INPUTS_CREATE)
                .hasMessageContaining(DASHBOARDS_CREATE);
    }

    @Test
    void validatePermissionsAndRolesAcceptsRequestWithoutRoles() throws NotFoundException {
        // `roles` is @Nullable in CreateUserRequest.
        when(roleService.loadByNames(List.of())).thenReturn(Set.of());
        currentUserHolds(STREAMS_READ);

        final var request = createUserRequest(null, List.of(STREAMS_READ));

        assertThatCode(() -> validator.validatePermissionsAndRoles(request, userContext)).doesNotThrowAnyException();
    }

    @Test
    void validatePermissionsAndRolesAcceptsRequestWithoutRolesOrPermissions() throws NotFoundException {
        when(roleService.loadByNames(List.of())).thenReturn(Set.of());

        final var request = createUserRequest(null, List.of());

        assertThatCode(() -> validator.validatePermissionsAndRoles(request, userContext)).doesNotThrowAnyException();
    }

    @Test
    void validatePermissionsAndRolesPropagatesForbiddenForUnreadableRoles() {
        final var request = createUserRequest(List.of(ADMIN_ROLE), List.of());

        assertThatThrownBy(() -> validator.validatePermissionsAndRoles(request, userContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not allowed to read roles: " + ADMIN_ROLE);
    }

    // ----------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------

    private Role roleGranting(String... permissions) {
        final Role role = mock(Role.class);
        when(role.getPermissions()).thenReturn(Set.of(permissions));
        return role;
    }

    private void allowReadingRoles(String... normalizedRoleNames) {
        for (final String roleName : normalizedRoleNames) {
            when(userContext.isPermitted(ROLES_READ, roleName)).thenReturn(true);
        }
    }

    private void currentUserHolds(String... permissions) {
        for (final String permission : permissions) {
            when(userContext.isPermitted(permission)).thenReturn(true);
        }
    }

    private CreateUserRequest createUserRequest(List<String> roles, List<String> permissions) {
        return CreateUserRequest.create("jane", "password", "jane@graylog.com", "Jane", "Doe",
                permissions, "Europe/Berlin", 1000L, null, roles, false);
    }
}
