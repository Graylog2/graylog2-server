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
package org.graylog.security.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.Capability;
import org.graylog.security.permissions.GRNPermission;
import org.graylog.security.shares.EntitySharesService;
import org.graylog.security.shares.Grantee;
import org.graylog.security.shares.GranteeShare;
import org.graylog.security.shares.GranteeSharesService;
import org.graylog.security.shares.ShareSummary;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitySharesResourceTest {
    private static final String USER_ID = "54e3deadbeefdeadbeef0001";

    private final GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();
    private final GRN ownedStream = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0a01");
    private final GRN ownedDashboard = grnRegistry.parse("grn::::dashboard:54e3deadbeefdeadbeef0a02");
    private final GRN notOwnedStream = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0a03");

    @Mock
    private UserService userService;
    @Mock
    private GranteeSharesService granteeSharesService;
    @Mock
    private EntitySharesService entitySharesService;
    @Mock
    private Subject subject;

    private User currentUser;
    private EntitySharesResource resource;

    @BeforeEach
    void setUp() {
        currentUser = mock(User.class);
        lenient().when(subject.getPrincipal()).thenReturn(USER_ID);
        lenient().when(userService.loadById(USER_ID)).thenReturn(currentUser);
        lenient().when(subject.isPermitted(any(Permission.class))).thenAnswer(invocation -> {
            final GRNPermission permission = invocation.getArgument(0);
            return Set.of(ownedStream, ownedDashboard).contains(permission.target());
        });

        resource = new EntitySharesResource(grnRegistry, userService, granteeSharesService, entitySharesService) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
    }

    @Test
    void summariesOnlyIncludeOwnedEntities() {
        when(entitySharesService.summarize(Set.of(ownedStream, ownedDashboard), currentUser))
                .thenReturn(Map.of(ownedStream, new ShareSummary(1, 0, 3)));

        final var response = resource.getSummaries(new EntitySharesResource.ShareSummaryRequest(Set.of(ownedStream, ownedDashboard, notOwnedStream)));

        assertThat(response.summaries()).containsOnly(Map.entry(ownedStream, new ShareSummary(1, 0, 3)));
    }

    @Test
    void summaryRequestWithMalformedGRNFailsToDeserialize() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();

        assertThatThrownBy(() -> objectMapper.readValue("{\"grns\": [\"not-a-grn\"]}", EntitySharesResource.ShareSummaryRequest.class))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    void granteesForOwnedEntity() {
        final List<GranteeShare> grantees = List.of(new GranteeShare(Grantee.createGlobal(), Capability.VIEW));
        when(entitySharesService.getGrantees(ownedStream, currentUser)).thenReturn(grantees);

        assertThat(resource.getGrantees(ownedStream.toString()).grantees()).isEqualTo(grantees);
    }

    @Test
    void granteesForNotOwnedEntity() {
        assertThatThrownBy(() -> resource.getGrantees(notOwnedStream.toString()))
                .isInstanceOf(ForbiddenException.class);
        verify(entitySharesService, never()).getGrantees(any(), any());
    }

    @Test
    void granteesForMalformedGRN() {
        assertThatThrownBy(() -> resource.getGrantees("not-a-grn"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> resource.getGrantees("grn::::foo:54e3deadbeefdeadbeef0a01"))
                .isInstanceOf(BadRequestException.class);
        verify(entitySharesService, never()).getGrantees(any(), any());
    }
}
