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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.apache.shiro.subject.Subject;
import org.graylog.collectors.CollectorIngestInputService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsInitializer;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog.collectors.TokenSigningKey;
import org.graylog.collectors.db.MarkerType;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.security.encryption.EncryptedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private HttpConfiguration httpConfiguration;
    @Mock
    private FleetService fleetService;
    @Mock
    private FleetTransactionLogService fleetTransactionLogService;
    @Mock
    private CollectorsInitializer collectorsInitializer;
    @Mock
    private ContainerRequestContext requestContext;

    private CollectorsConfigResource resource;

    @BeforeEach
    void setUp() {
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com:443/"));
        resource = new CollectorsConfigResource(
                collectorsConfigService,
                collectorIngestInputService,
                httpConfiguration,
                fleetService,
                fleetTransactionLogService,
                collectorsInitializer
        );
    }

    // --- GET ---

    @Test
    void getReturnsExistingConfig() {
        final var existing = CollectorsConfig.createDefault("graylog.example.com");
        when(collectorsConfigService.get()).thenReturn(Optional.of(existing));

        assertThat(resource.get(requestContext)).isEqualTo(existing);
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

    // --- PUT: bootstrap vs update ---

    @Test
    void putBootstrapsViaInitializerWhenConfigAbsent() throws Exception {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
        when(collectorsInitializer.initialize(any())).thenAnswer(returnsFirstArg());

        resource.put(request(null, null, null, false));

        verify(collectorsInitializer).initialize(any(CollectorsConfig.class));
        verify(collectorsConfigService).save(any(CollectorsConfig.class));
    }

    @Test
    void putUpdatesExistingConfigWithoutInitializing() throws Exception {
        final var existingKey = new TokenSigningKey(EncryptedValue.createUnset(), "pub", "existing-fp", Instant.now());
        final var existing = CollectorsConfig.builder()
                .caCertId("existing-ca")
                .signingCertId("existing-signing")
                .otlpServerCertId("existing-otlp")
                .tokenSigningKey(existingKey)
                .http(new IngestEndpointConfig("host", 14401))
                .build();
        when(collectorsConfigService.get()).thenReturn(Optional.of(existing));

        final var result = resource.put(request(Duration.ofMinutes(10), null, null, false));

        verify(collectorsInitializer, never()).initialize(any());
        verify(collectorsConfigService).save(any(CollectorsConfig.class));
        // System fields (certs + token) are carried forward; only the overlaid field changes.
        assertThat(result.caCertId()).isEqualTo("existing-ca");
        assertThat(result.tokenSigningKey()).isEqualTo(existingKey);
        assertThat(result.collectorOfflineThreshold()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void putKeepsOmittedThresholdsOnUpdate() throws Exception {
        final var existing = CollectorsConfig.builder()
                .caCertId("existing-ca")
                .tokenSigningKey(new TokenSigningKey(EncryptedValue.createUnset(), "pub", "fp", Instant.now()))
                .http(new IngestEndpointConfig("host", 14401))
                .collectorOfflineThreshold(Duration.ofMinutes(7))
                .collectorDefaultVisibilityThreshold(Duration.ofDays(3))
                .collectorExpirationThreshold(Duration.ofDays(9))
                .build();
        when(collectorsConfigService.get()).thenReturn(Optional.of(existing));

        // Only the offline threshold is provided; the others keep their existing values (PATCH semantics).
        final var result = resource.put(request(Duration.ofMinutes(10), null, null, false));

        assertThat(result.collectorOfflineThreshold()).isEqualTo(Duration.ofMinutes(10));
        assertThat(result.collectorDefaultVisibilityThreshold()).isEqualTo(Duration.ofDays(3));
        assertThat(result.collectorExpirationThreshold()).isEqualTo(Duration.ofDays(9));
    }

    @Test
    void putAppendsIngestMarkerForExistingFleets() throws Exception {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
        when(collectorsInitializer.initialize(any())).thenAnswer(returnsFirstArg());
        final var fleetIds = Set.of("fleet-1", "fleet-2");
        when(fleetService.getAllFleetIds()).thenReturn(fleetIds);

        resource.put(request(null, null, null, false));

        verify(fleetTransactionLogService).appendFleetMarker(eq(fleetIds), eq(MarkerType.INGEST_CONFIG_CHANGED));
    }

    @Test
    void putAcceptsValidThresholds() throws Exception {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
        when(collectorsInitializer.initialize(any())).thenAnswer(returnsFirstArg());

        final var result = resource.put(request(Duration.ofMinutes(10), Duration.ofHours(12), Duration.ofDays(3), false));

        assertThat(result.collectorOfflineThreshold()).isEqualTo(Duration.ofMinutes(10));
        assertThat(result.collectorDefaultVisibilityThreshold()).isEqualTo(Duration.ofHours(12));
        assertThat(result.collectorExpirationThreshold()).isEqualTo(Duration.ofDays(3));
    }

    @Test
    void putAppliesDefaultsForOmittedThresholdsOnBootstrap() throws Exception {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
        when(collectorsInitializer.initialize(any())).thenAnswer(returnsFirstArg());

        final var result = resource.put(request(null, null, null, false));

        assertThat(result.collectorOfflineThreshold()).isEqualTo(CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD);
        assertThat(result.collectorDefaultVisibilityThreshold()).isEqualTo(CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD);
        assertThat(result.collectorExpirationThreshold()).isEqualTo(CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD);
    }

    // --- PUT: threshold validation (rejected before any bootstrap/save) ---

    @Test
    void putRejectsZeroVisibilityThreshold() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.put(request(null, Duration.ZERO, null, false)))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(error((ValidationException) ex, "collector_default_visibility_threshold"))
                        .isEqualTo("Must be a positive duration"));

        verify(collectorsInitializer, never()).initialize(any());
        verify(collectorsConfigService, never()).save(any());
    }

    @Test
    void putRejectsVisibilityThresholdBelowOfflineThreshold() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.put(request(null, Duration.ofMinutes(3), null, false)))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(error((ValidationException) ex, "collector_default_visibility_threshold"))
                        .isEqualTo("Must be greater than the offline threshold (5 minutes)"));
    }

    @Test
    void putRejectsExpirationNotGreaterThanVisibility() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.put(request(null, Duration.ofDays(2), Duration.ofDays(1), false)))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(error((ValidationException) ex, "collector_expiration_threshold"))
                        .isEqualTo("Must be greater than the visibility threshold (2 days)"));
    }

    @Test
    void putRejectsOfflineThresholdBelowOneMinute() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.put(request(Duration.ofSeconds(30), null, null, false)))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .containsKey("collector_offline_threshold"));
    }

    @Test
    void putRejectsMultipleInvalidThresholds() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.put(request(null, Duration.ofMinutes(-5), Duration.ofMinutes(-10), false)))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    final var errors = ((ValidationException) ex).getErrors();
                    assertThat(errors).containsKey("collector_default_visibility_threshold");
                    assertThat(errors).containsKey("collector_expiration_threshold");
                });
    }

    // --- PUT: input creation ---

    @Test
    void putWithCreateInputDelegatesToService() throws Exception {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
        when(collectorsInitializer.initialize(any())).thenAnswer(returnsFirstArg());

        resource.put(request(null, null, null, true));

        verify(collectorIngestInputService).createInput(any(Subject.class), eq("admin"), eq(14401));
        verify(collectorsConfigService).save(any(CollectorsConfig.class));
    }

    @Test
    void putWithCreateInputFalseDoesNotCallService() throws Exception {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());
        when(collectorsInitializer.initialize(any())).thenAnswer(returnsFirstArg());

        resource.put(request(null, null, null, false));

        verify(collectorIngestInputService, never()).createInput(any(), any(), any(int.class));
        verify(collectorsConfigService).save(any(CollectorsConfig.class));
    }

    // --- POST /inputs ---

    @Test
    void createInputDelegatesToServiceUsingSavedConfigPort() throws Exception {
        final var existing = CollectorsConfig.builder()
                .caCertId("ca-id")
                .tokenSigningKey(new TokenSigningKey(EncryptedValue.createUnset(), "pub", "fp", Instant.now()))
                .http(new IngestEndpointConfig("host", 14445))
                .build();
        when(collectorsConfigService.get()).thenReturn(Optional.of(existing));
        when(collectorIngestInputService.getInputIds()).thenReturn(List.of("input-new"));

        final var result = resource.createInput();

        verify(collectorIngestInputService).createInput(any(Subject.class), eq("admin"), eq(14445));
        assertThat(result.collectorInputIds()).containsExactly("input-new");
    }

    @Test
    void createInputFailsWhenNoConfigExists() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.createInput())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not been initialized");
    }

    private static CollectorsConfigRequest request(Duration offline, Duration visibility, Duration expiration,
                                                    boolean createInput) {
        return new CollectorsConfigRequest(
                new CollectorsConfigRequest.IngestEndpointRequest("host", 14401),
                offline, visibility, expiration, createInput);
    }

    private static String error(ValidationException ex, String field) {
        return ((ValidationResult.ValidationFailed) ex.getErrors().get(field).get(0)).getError();
    }
}
