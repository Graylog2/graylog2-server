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

import jakarta.ws.rs.ForbiddenException;
import org.bson.types.ObjectId;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.PaginatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    @InjectMocks
    private AuthzRolesResource classUnderTest;

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
}
