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
package org.graylog.collectors.opamp.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.ComputedFieldRegistry;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentTokenResourceTest {

    @Mock
    private EnrollmentTokenService enrollmentTokenService;
    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private UserService userService;

    private EnrollmentTokenResource resource;

    @BeforeEach
    void setUp() throws Exception {
        final var mockUser = mock(User.class);
        lenient().when(mockUser.getId()).thenReturn("test-user-id");
        lenient().when(mockUser.getName()).thenReturn("testuser");

        lenient().when(userService.loadById("test-principal")).thenReturn(mockUser);

        final var subject = mock(Subject.class);
        lenient().when(subject.getPrincipal()).thenReturn("test-principal");
        lenient().when(subject.isAuthenticated()).thenReturn(true);
        ThreadContext.bind(subject);

        resource = new EnrollmentTokenResource(
                enrollmentTokenService, clusterConfigService, mock(ComputedFieldRegistry.class));

        // Set userService and securityContext on RestResource using VarHandle (setAccessible is forbidden)
        final var lookup = MethodHandles.privateLookupIn(RestResource.class, MethodHandles.lookup());

        final VarHandle userServiceHandle = lookup.findVarHandle(RestResource.class, "userService", UserService.class);
        userServiceHandle.set(resource, userService);

        final var securityContext = new ShiroSecurityContext(subject, null, true, null, new MultivaluedHashMap<>());
        final VarHandle securityContextHandle = lookup.findVarHandle(RestResource.class, "securityContext", SecurityContext.class);
        securityContextHandle.set(resource, securityContext);
    }

    @AfterEach
    void tearDown() {
        ThreadContext.unbindSubject();
        ThreadContext.unbindSecurityManager();
    }

    @Test
    void createTokenDelegatesToService() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(
                new CollectorsConfig("ca-id", "token-id", "otlp-id",
                        new IngestEndpointConfig(true, "host", 14401, "input-1"),
                        new IngestEndpointConfig(false, "host", 14402, null),
                        CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD,
                        CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD,
                        CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD));

        final var request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );
        final var expectedResponse = new EnrollmentTokenResponse(
                "test-token",
                Instant.now().plusSeconds(86400)
        );

        when(enrollmentTokenService.createToken(eq(request), any(EnrollmentTokenCreator.class)))
                .thenReturn(expectedResponse);

        final var response = resource.createToken(request);

        assertThat(response).isEqualTo(expectedResponse);
        verify(enrollmentTokenService).createToken(eq(request), any(EnrollmentTokenCreator.class));
    }

    @Test
    void createTokenThrowsWhenCollectorsNotConfigured() {
        when(clusterConfigService.get(CollectorsConfig.class)).thenReturn(null);

        final var request = new CreateEnrollmentTokenRequest(
                "test-fleet",
                Duration.ofDays(1)
        );

        assertThatThrownBy(() -> resource.createToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Collectors must be configured");
    }

    @Test
    void listReturnsPaginatedTokens() {
        final var token = new EnrollmentTokenDTO(
                "token-id", "jti-1", "kid-1", "fleet-1",
                new EnrollmentTokenCreator("test-user-id", "testuser"),
                Instant.now(), Instant.now().plusSeconds(86400), 0, null);

        final var paginatedList = new PaginatedList<>(List.of(token), 1, 1, 50);

        when(enrollmentTokenService.findPaginated(any(), any(DbSortResolver.ResolvedSort.class), anyInt(), anyInt()))
                .thenReturn(paginatedList);

        final var response = resource.list(1, 50, "", List.of(), "created_at",
                org.graylog2.rest.models.SortOrder.DESCENDING);

        assertThat(response.elements()).hasSize(1);
        assertThat(response.elements().getFirst()).isEqualTo(token);
    }

    @Test
    void deleteReturns204WhenTokenExists() {
        when(enrollmentTokenService.delete("token-id")).thenReturn(true);

        final Response response = resource.delete("token-id");

        assertThat(response.getStatus()).isEqualTo(204);
        verify(enrollmentTokenService).delete("token-id");
    }

    @Test
    void deleteReturns404WhenTokenNotFound() {
        when(enrollmentTokenService.delete("nonexistent")).thenReturn(false);

        assertThatThrownBy(() -> resource.delete("nonexistent"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Enrollment token not found");
    }
}
