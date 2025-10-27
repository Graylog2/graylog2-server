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
package org.graylog.security.authservice.rest;

import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.AuthServiceBackendUsageCheck;
import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserOverviewDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
class AuthServiceBackendsResourceTest {

    @Mock
    private DBAuthServiceBackendService dbService;
    @Mock
    private GlobalAuthServiceConfig globalAuthServiceConfig;
    @Mock
    private PaginatedUserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private AuthServiceBackendUsageCheck usageCheck;
    @Mock
    private SearchQueryParser userSearchQueryParser;

    @InjectMocks
    private AuthServiceBackendsResource classUnderTest;

    @Test
    @WithAuthorization(permissions = {RestPermissions.AUTH_SERVICE_GLOBAL_CONFIG_READ, RestPermissions.USERS_READ, "roles:read:reader"})
    void testGetUsers() throws NotFoundException {
        String backendId = "backend-1";
        int page = 1;
        int perPage = 2;
        String query = "";
        String sort = UserOverviewDTO.FIELD_FULL_NAME;
        SortOrder order = SortOrder.ASCENDING;

        AuthServiceBackendDTO backendDTO = Mockito.mock(AuthServiceBackendDTO.class);
        Mockito.when(backendDTO.id()).thenReturn(backendId);

        Mockito.when(dbService.get(backendId)).thenReturn(Optional.of(backendDTO));

        UserOverviewDTO user1 = Mockito.mock(UserOverviewDTO.class);
        UserOverviewDTO user2 = Mockito.mock(UserOverviewDTO.class);
        List<UserOverviewDTO> users = List.of(user1, user2);
        PaginatedList<UserOverviewDTO> paginatedList = new PaginatedList<>(users, users.size(), page, perPage);

        Mockito.when(userService.findPaginatedByAuthServiceBackend(
                Mockito.any(), Mockito.eq(page), Mockito.eq(perPage), Mockito.eq(sort), Mockito.eq(order), Mockito.eq(backendId)
        )).thenReturn(paginatedList);

        Mockito.when(user1.roles()).thenReturn(Set.of("reader"));
        Mockito.when(user2.roles()).thenReturn(Set.of("admin"));

        Role readerRole = Mockito.mock(Role.class);
        Role adminRole = Mockito.mock(Role.class);

        Mockito.when(readerRole.getId()).thenReturn("readerId");
        Mockito.when(readerRole.getName()).thenReturn("reader");
        Mockito.when(adminRole.getId()).thenReturn("amindId");
        Mockito.when(adminRole.getName()).thenReturn("admin");

        Mockito.when(roleService.findIdMap(Mockito.anySet())).thenReturn(Map.of("reader", readerRole, "admin", adminRole));

        PaginatedResponse<UserOverviewDTO> response = classUnderTest.getUsers(page, perPage, query, sort, order, backendId);

        Assertions.assertNotNull(response);
        PaginatedList<UserOverviewDTO> userOverviewDTOS = response.paginatedList();
        Assertions.assertEquals(2, userOverviewDTOS.size());
        Map<String, Object> context = (Map<String, Object>) response.jsonValue().get("context");
        Map<String, Object> roles = (Map<String, Object>) context.get("roles");
        Set<String> titles = roles.values().stream()
                .map(obj -> ((Map<?, ?>) obj).get("title"))
                .map(String.class::cast)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of("reader", "unknown"), titles);
    }

}
