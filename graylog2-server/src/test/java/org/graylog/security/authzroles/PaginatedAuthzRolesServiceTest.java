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

import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mongojack.DBQuery;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("roles.json")
class PaginatedAuthzRolesServiceTest {
    private PaginatedAuthzRolesService service;
    private UserService userService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoObjectMapperProvider,
               @Mock UserService userService) {
        this.userService = userService;
        this.service = new PaginatedAuthzRolesService(mongodb.mongoConnection(), userService, mongoObjectMapperProvider);
    }

    @Test
    void count() {
        assertThat(service.count()).isEqualTo(16L);
    }

    @Test
    void findPaginatedByIds(@Mock SearchQuery searchQuery) {
        final String testRoleId = "56701ac4c8302ff6bee2a65d";
        final String readerRoleId = "564c6707c8306e079f718980";
        final String archiveManagerRoleId = "58dbaa158ae4923256dc6265";
        final Set<String> roleIds = ImmutableSet.of(testRoleId, readerRoleId, archiveManagerRoleId);

        when(searchQuery.toDBQuery()).thenReturn(DBQuery.empty());

        final PaginatedList<AuthzRoleDTO> result = service.findPaginatedByIds(
                searchQuery,
                1,
                10,
                "id",
                "asc",
                roleIds
        );

        assertThat(result.delegate().size()).isEqualTo(3);
        assertThat(result.delegate().get(0)).satisfies(role -> {
            assertThat(role.id()).isEqualTo(readerRoleId);
            assertThat(role.name()).isEqualTo("Reader");
        });
        assertThat(result.delegate().get(1)).satisfies(role -> {
            assertThat(role.id()).isEqualTo(testRoleId);
            assertThat(role.name()).isEqualTo("Test Role");
        });
        assertThat(result.delegate().get(2)).satisfies(role -> {
            assertThat(role.id()).isEqualTo(archiveManagerRoleId);
            assertThat(role.name()).isEqualTo("Archive Manager");
        });
        assertThat(result.grandTotal()).get().isEqualTo(16L);
        assertThat(result.pagination()).satisfies(paginationInfo -> {
            assertThat(paginationInfo.count()).isEqualTo(3);
            assertThat(paginationInfo.total()).isEqualTo(3);
            assertThat(paginationInfo.page()).isEqualTo(1);
            assertThat(paginationInfo.perPage()).isEqualTo(10);
        });
    }

    @Test
    void findPaginatedByIdsWithFilter(@Mock SearchQuery searchQuery) {
        final String testRoleId = "56701ac4c8302ff6bee2a65d";
        final String readerRoleId = "564c6707c8306e079f718980";
        final String archiveManagerRoleId = "58dbaa158ae4923256dc6265";
        final Set<String> roleIds = ImmutableSet.of(testRoleId, readerRoleId, archiveManagerRoleId);

        when(searchQuery.toDBQuery()).thenReturn(DBQuery.empty());

        final PaginatedList<AuthzRoleDTO> result = service.findPaginatedByIdsWithFilter(
                searchQuery,
                (role) -> testRoleId.equals(role.id()),
                1,
                10,
                "id",
                "asc",
                roleIds
        );

        assertThat(result.delegate().size()).isEqualTo(1);
        assertThat(result.delegate().get(0)).satisfies(role -> {
            assertThat(role.id()).isEqualTo(testRoleId);
            assertThat(role.name()).isEqualTo("Test Role");
        });
        assertThat(result.grandTotal()).get().isEqualTo(16L);
        assertThat(result.pagination()).satisfies(paginationInfo -> {
            assertThat(paginationInfo.count()).isEqualTo(1);
            assertThat(paginationInfo.total()).isEqualTo(1);
            assertThat(paginationInfo.page()).isEqualTo(1);
            assertThat(paginationInfo.perPage()).isEqualTo(10);
        });
    }

    @Test
    void findByIds() {
        final List<AuthzRoleDTO> roles = service.findByIds(ImmutableSet.of(
                "5d41bb973086a840541a3ed2",
                "564c6707c8306e079f718980"
        ));

        assertThat(roles).hasSize(2);

        assertThat(roles.get(0).id()).isEqualTo("564c6707c8306e079f718980");
        assertThat(roles.get(0).name()).isEqualTo("Reader");

        assertThat(roles.get(1).id()).isEqualTo("5d41bb973086a840541a3ed2");
        assertThat(roles.get(1).name()).isEqualTo("Alerts Manager");
    }

    @Test
    void getAllRoleIds() {
        assertThat(service.getAllRoleIds()).isEqualTo(ImmutableSet.of(
                "564c6707c8306e079f718980",
                "56701ac4c8302ff6bee2a65d",
                "5b17d7c63f3ab8204eea0589",
                "58dbaa158ae4923256dc6266",
                "564c6707c8306e079f71897f",
                "58dbaa158ae4923256dc6265",
                "59fc4b2b6e948411fadbd85d",
                "59fc4b2b6e948411fadbd85e",
                "5c2f6d3b3dd06601be176b85",
                "5c488b67e3f1420b4d9ae635",
                "5c488f1de3f14219be1cb9f6",
                "5d41bb973086a840541a3ed2",
                "5f1f0d2a6f58d7c052d49775",
                "5f1f0d2a6f58d7c052d49778",
                "5f1f0d2a6f58d7c052d4977b",
                "5f22792d6f58d7c0521edb23"
        ));
    }

    @Test
    void delete() {
        final String roleId = "5d41bb973086a840541a3ed2";
        final Optional<AuthzRoleDTO> role = service.get(roleId);

        assertThat(role).isPresent();

        service.delete(roleId);

        verify(userService).dissociateAllUsersFromRole(role.get().toLegacyRole());
    }
}
