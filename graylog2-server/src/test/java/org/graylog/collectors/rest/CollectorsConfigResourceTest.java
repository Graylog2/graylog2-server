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
package org.graylog.collectors.rest;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.apache.shiro.subject.Subject;
import org.graylog.collectors.CollectorCaService;
import org.graylog.collectors.CollectorIngestInputService;
import org.graylog.collectors.CollectorLogsDestinationService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.TokenSigningKey;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.security.pki.CertificateEntry;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
@WithAuthorization(username = "admin", permissions = "*")
class CollectorsConfigResourceTest {

    @Mock
    private CollectorsConfigService collectorsConfigService;
    @Mock
    private CollectorIngestInputService collectorIngestInputService;
    @Mock
    private CollectorLogsDestinationService collectorLogsDestinationService;
    @Mock
    private HttpConfiguration httpConfiguration;
    @Mock
    private CollectorCaService collectorCaService;
    @Mock
    private EnrollmentTokenService enrollmentTokenService;
    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private FleetService fleetService;
    @Mock
    private FleetTransactionLogService fleetTransactionLogService;

    private CollectorsConfigResource resource;

    @BeforeEach
    void setUp() {
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com:443/"));
        resource = new CollectorsConfigResource(
                collectorsConfigService,
                collectorIngestInputService,
                collectorLogsDestinationService,
                httpConfiguration,
                fleetService,
                fleetTransactionLogService,
                enrollmentTokenService,
                collectorCaService
        );
    }

    @Test
    void getReturnsExistingConfig() {
        final var existing = CollectorsConfig.createDefault("graylog.example.com");
        when(collectorsConfigService.get()).thenReturn(Optional.of(existing));

        final var result = resource.get(requestContext);

        assertThat(result).isEqualTo(existing);
    }

    @Test
    void getReturnsDefaultWhenNoConfigExists() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
        when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());

        final var result = resource.get(requestContext);

        assertThat(result.http().hostname()).isEqualTo("graylog.example.com");
        assertThat(result.http().port()).isEqualTo(14401);
        assertThat(result.collectorOfflineThreshold()).isEqualTo(CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD);
        assertThat(result.collectorDefaultVisibilityThreshold()).isEqualTo(CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD);
        assertThat(result.collectorExpirationThreshold()).isEqualTo(CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD);
    }

    @Test
    void putInitializesCaAndDestination() throws ValidationException {
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, null, null, null
        );

        resource.put(request);

        verify(collectorCaService).initializeCa();
        verify(collectorLogsDestinationService).ensureExists();
    }

    @Test
    void putPersistsConfig() throws ValidationException {
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, null, null, null
        );

        final var fleetIds = Set.of("fleet-1", "fleet-2");
        when(fleetService.getAllFleetIds()).thenReturn(fleetIds);

        resource.put(request);

        verify(collectorsConfigService).save(any(CollectorsConfig.class));
        verify(fleetTransactionLogService).appendFleetMarker(eq(fleetIds), eq(MarkerType.INGEST_CONFIG_CHANGED));
    }

    @Test
    void putRejectsZeroVisibilityThreshold() {
        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, Duration.ZERO, null, null
        );

        assertThatThrownBy(() -> resource.put(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    final var errors = ((ValidationException) ex).getErrors();
                    assertThat(errors).containsKey("collector_default_visibility_threshold");
                    assertThat(((ValidationResult.ValidationFailed) errors.get("collector_default_visibility_threshold").get(0)).getError())
                            .isEqualTo("Must be a positive duration");
                });
    }

    @Test
    void putRejectsVisibilityThresholdBelowOfflineThreshold() {
        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, Duration.ofMinutes(3), null, null
        );

        assertThatThrownBy(() -> resource.put(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    final var errors = ((ValidationException) ex).getErrors();
                    assertThat(errors).containsKey("collector_default_visibility_threshold");
                    assertThat(((ValidationResult.ValidationFailed) errors.get("collector_default_visibility_threshold").get(0)).getError())
                            .isEqualTo("Must be greater than the offline threshold (5 minutes)");
                });
    }

    @Test
    void putRejectsExpirationNotGreaterThanVisibility() {
        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, Duration.ofDays(2), Duration.ofDays(1), null
        );

        assertThatThrownBy(() -> resource.put(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    final var errors = ((ValidationException) ex).getErrors();
                    assertThat(errors).containsKey("collector_expiration_threshold");
                    assertThat(((ValidationResult.ValidationFailed) errors.get("collector_expiration_threshold").get(0)).getError())
                            .isEqualTo("Must be greater than the visibility threshold (2 days)");
                });
    }

    @Test
    void putRejectsMultipleInvalidThresholds() {
        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, Duration.ofMinutes(-5), Duration.ofMinutes(-10), null
        );

        assertThatThrownBy(() -> resource.put(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    final var errors = ((ValidationException) ex).getErrors();
                    assertThat(errors).containsKey("collector_default_visibility_threshold");
                    assertThat(errors).containsKey("collector_expiration_threshold");
                });
    }

    @Test
    void putAcceptsValidThresholds() throws ValidationException {
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                Duration.ofMinutes(10), Duration.ofHours(12), Duration.ofDays(3), null
        );

        final var result = resource.put(request);

        assertThat(result.collectorOfflineThreshold()).isEqualTo(Duration.ofMinutes(10));
        assertThat(result.collectorDefaultVisibilityThreshold()).isEqualTo(Duration.ofHours(12));
        assertThat(result.collectorExpirationThreshold()).isEqualTo(Duration.ofDays(3));
    }

    @Test
    void putAcceptsNullThresholds() throws ValidationException {
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, null, null, null
        );

        final var result = resource.put(request);

        assertThat(result.collectorOfflineThreshold()).isEqualTo(CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD);
        assertThat(result.collectorDefaultVisibilityThreshold()).isEqualTo(CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD);
        assertThat(result.collectorExpirationThreshold()).isEqualTo(CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD);
    }

    @Test
    void putRejectsOfflineThresholdBelowOneMinute() {
        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                Duration.ofSeconds(30), null, null, null
        );

        assertThatThrownBy(() -> resource.put(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    final var errors = ((ValidationException) ex).getErrors();
                    assertThat(errors).containsKey("collector_offline_threshold");
                });
    }

    @Test
    void putCreatesNewTokenSigningKeyWhenNoExistingConfig() throws Exception {
        final var expectedKey = new TokenSigningKey(EncryptedValue.createUnset(), "pubkey", "fingerprint-1", Instant.now());
        when(enrollmentTokenService.createTokenSigningKey()).thenReturn(expectedKey);
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, null, null, null
        );

        final var result = resource.put(request);

        verify(enrollmentTokenService).createTokenSigningKey();
        assertThat(result.tokenSigningKey()).isEqualTo(expectedKey);
    }

    @Test
    void putReusesTokenSigningKeyFromExistingConfig() throws Exception {
        final var existingKey = new TokenSigningKey(EncryptedValue.createUnset(), "pubkey", "existing-fingerprint", Instant.now());
        final var existingConfig = CollectorsConfig.builder()
                .caCertId("ca-id")
                .otlpServerCertId("otlp-id")
                .tokenSigningKey(existingKey)
                .http(new CollectorsConfigRequest.IngestEndpointRequest("host", 14401).toConfig())
                .collectorOfflineThreshold(CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD)
                .collectorDefaultVisibilityThreshold(CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD)
                .collectorExpirationThreshold(CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD)
                .build();

        when(collectorsConfigService.get()).thenReturn(Optional.of(existingConfig));
        stubInitAndLoad();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, null, null, null
        );

        final var result = resource.put(request);

        assertThat(result.tokenSigningKey()).isEqualTo(existingKey);
    }

    @Test
    void putThrowsInternalServerErrorWhenTokenSigningKeyCreationFails() throws Exception {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
        when(enrollmentTokenService.createTokenSigningKey()).thenThrow(new NoSuchAlgorithmException("test error"));

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, null, null, null
        );

        assertThatThrownBy(() -> resource.put(request))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("Could not create token signing key");
    }

    @Test
    void putWithCreateInputDelegatesToService() throws Exception {
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, null, null, true
        );

        resource.put(request);

        verify(collectorIngestInputService).createInput(any(Subject.class), eq("admin"), eq(14401));
        verify(collectorsConfigService).save(any(CollectorsConfig.class));
    }

    @Test
    void putWithCreateInputNullDoesNotCallService() throws Exception {
        stubCaService();

        final var request = new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                null, null, null, null
        );

        resource.put(request);

        verify(collectorIngestInputService, never()).createInput(any(), any(), any(int.class));
        verify(collectorsConfigService).save(any(CollectorsConfig.class));
    }

    private void stubCaService() {
        lenient().when(collectorsConfigService.get()).thenReturn(Optional.empty());
        stubInitAndLoad();
    }

    private void stubInitAndLoad() {
        final var caCert = mock(CertificateEntry.class);
        final var signingCert = mock(CertificateEntry.class);
        final var otlpServerCert = mock(CertificateEntry.class);
        final var hierarchy = new CollectorCaService.CaHierarchy(caCert, signingCert, otlpServerCert);

        lenient().when(caCert.id()).thenReturn("ca-id");
        lenient().when(signingCert.id()).thenReturn("signing-cert-id");
        lenient().when(otlpServerCert.id()).thenReturn("otlp-id");
        lenient().when(collectorCaService.initializeCa()).thenReturn(hierarchy);
        lenient().when(collectorCaService.loadHierarchy()).thenReturn(hierarchy);
    }
}
