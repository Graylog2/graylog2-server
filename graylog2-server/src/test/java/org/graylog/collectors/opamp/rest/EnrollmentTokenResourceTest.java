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
import jakarta.ws.rs.core.Response;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.ComputedFieldRegistry;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
@WithAuthorization(permissions = "*")
class EnrollmentTokenResourceTest {

    @Mock
    private EnrollmentTokenService enrollmentTokenService;
    @Mock
    private CollectorsConfigService collectorsConfigService;
    @Mock
    private FleetService fleetService;

    private EnrollmentTokenResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new EnrollmentTokenResource(enrollmentTokenService, collectorsConfigService, fleetService,
                mock(ComputedFieldRegistry.class), new NullAuditEventSender());
    }

    @Test
    void createTokenDelegatesToService() {
        when(collectorsConfigService.get()).thenReturn(Optional.of(
                CollectorsConfig.createDefaultBuilder("host")
                        .caCertId("ca-cert-id")
                        .signingCertId("signing-cert-id")
                        .otlpServerCertId("otlp-id")
                        .build()));
        when(fleetService.get("test-fleet")).thenReturn(java.util.Optional.of(mock(FleetDTO.class)));

        final var request = new CreateEnrollmentTokenRequest(
                "test-token",
                "test-fleet",
                Duration.ofDays(1)
        );
        final var expectedResponse = new EnrollmentTokenResponse(
                "1",
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
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final var request = new CreateEnrollmentTokenRequest(
                "test-token",
                "test-fleet",
                Duration.ofDays(1)
        );

        assertThatThrownBy(() -> resource.createToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Collectors must be configured");
    }

    @Test
    void createTokenThrowsWhenFleetNotFound() {
        when(collectorsConfigService.get()).thenReturn(Optional.of(
                CollectorsConfig.createDefaultBuilder("host")
                        .caCertId("ca-cert-id")
                        .signingCertId("signing-cert-id")
                        .otlpServerCertId("otlp-id")
                        .build()));
        when(fleetService.get("nonexistent-fleet")).thenReturn(java.util.Optional.empty());

        final var request = new CreateEnrollmentTokenRequest("test-token", "nonexistent-fleet", Duration.ofDays(1));

        assertThatThrownBy(() -> resource.createToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Fleet not found");
    }

    @Test
    void deleteReturnsBadRequestForInvalidIdFormat() {
        assertThatThrownBy(() -> resource.delete("not-a-valid-objectid"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid token ID format");
    }

    @Test
    void listReturnsPaginatedTokens() {
        final var token = createToken("token-id");

        final var paginatedList = new PaginatedList<>(List.of(token), 1, 1, 50);

        when(enrollmentTokenService.findPaginated(any(), any(DbSortResolver.ResolvedSort.class), anyInt(), anyInt(), any()))
                .thenReturn(paginatedList);

        final var response = resource.list(1, 50, "", List.of(), "created_at",
                org.graylog2.rest.models.SortOrder.DESCENDING);

        assertThat(response.elements()).hasSize(1);
        assertThat(response.elements().getFirst()).isEqualTo(token);
    }

    @Test
    void deleteReturns204WhenTokenExists() {
        final String validId = "507f1f77bcf86cd799439011";
        when(enrollmentTokenService.findOne(validId)).thenReturn(Optional.of(createToken(validId)));
        when(enrollmentTokenService.delete(validId)).thenReturn(true);

        final Response response = resource.delete(validId);

        assertThat(response.getStatus()).isEqualTo(204);
        verify(enrollmentTokenService).delete(validId);
    }

    @Test
    void deleteReturns404WhenTokenNotFound() {
        final String validId = "507f1f77bcf86cd799439012";
        when(enrollmentTokenService.findOne(validId)).thenReturn(Optional.of(createToken(validId)));
        when(enrollmentTokenService.delete(validId)).thenReturn(false);

        assertThatThrownBy(() -> resource.delete(validId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Enrollment token not found");
    }

    @Test
    void bulkDeleteDelegatesToService() {
        final var ids = List.of("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012");

        when(enrollmentTokenService.findByIds(ids)).thenReturn(ids.stream().map(this::createToken));
        when(enrollmentTokenService.deleteMany(ids)).thenReturn(2L);

        final var response = resource.bulkDelete(new BulkOperationRequest(ids));

        assertThat(response.successfullyPerformed()).isEqualTo(2);
        assertThat(response.failures()).isEmpty();
        verify(enrollmentTokenService).deleteMany(ids);
    }

    @Test
    void bulkDeleteThrowsWhenNoIdsProvided() {
        assertThatThrownBy(() -> resource.bulkDelete(new BulkOperationRequest(List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No IDs provided");
    }

    @Test
    void bulkDeleteThrowsWhenIdsNull() {
        assertThatThrownBy(() -> resource.bulkDelete(new BulkOperationRequest(null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No IDs provided");
    }

    @Test
    void bulkDeleteReportsPartialDeletion() {
        final var ids = List.of("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012", "507f1f77bcf86cd799439013");
        when(enrollmentTokenService.findByIds(ids)).thenReturn(ids.stream().map(this::createToken));
        when(enrollmentTokenService.deleteMany(ids)).thenReturn(2L);

        final var response = resource.bulkDelete(new BulkOperationRequest(ids));

        assertThat(response.successfullyPerformed()).isEqualTo(2);
    }

    private EnrollmentTokenDTO createToken(String id) {
        return EnrollmentTokenDTO.builder()
                .id(id)
                .name("test-token")
                .jti("jti")
                .kid("kid")
                .fleetId("fleet-id")
                .createdBy(new EnrollmentTokenCreator("test-user-id", "testuser"))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
    }
}
