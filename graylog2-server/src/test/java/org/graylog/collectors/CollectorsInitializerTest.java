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
package org.graylog.collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.pki.CertificateService;
import org.graylog.testing.TestClocks;
import org.graylog.testing.cluster.ClusterConfigServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.time.Clock;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(ClusterConfigServiceExtension.class)
class CollectorsInitializerTest {
    private final Clock clock = TestClocks.fixedEpoch();

    private CollectorCaService caService;
    private EnrollmentTokenService enrollmentTokenService;
    private CollectorsConfigService collectorsConfigService;
    private CollectorsInitializer initializer;

    @BeforeEach
    void setUp(MongoDBTestService mongodb, ClusterConfigService clusterConfigService) {
        final var encryptedValueService = new EncryptedValueService("1234567890abcdef");
        final ObjectMapper objectMapper = new ObjectMapperProvider(
                ObjectMapperProvider.class.getClassLoader(),
                Collections.emptySet(),
                encryptedValueService,
                GRNRegistry.createWithBuiltinTypes(),
                InputConfigurationBeanDeserializerModifier.withoutConfig()
        ).get();
        final var mongoCollections = new MongoCollections(
                new MongoJackObjectMapperProvider(objectMapper), mongodb.mongoConnection());

        final var clusterIdService = mock(ClusterIdService.class);
        when(clusterIdService.getString()).thenReturn("cluster-id");
        final var httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com:443/"));

        final var certificateService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty(), clock);
        collectorsConfigService = new CollectorsConfigService(clusterConfigService, mock(ClusterEventBus.class), httpConfiguration);
        caService = new CollectorCaService(certificateService, clusterIdService, collectorsConfigService, clock);
        enrollmentTokenService = new EnrollmentTokenService(clusterIdService, clock, encryptedValueService, collectorsConfigService, mongoCollections);

        initializer = new CollectorsInitializer(caService, enrollmentTokenService);
    }

    @Test
    void initializeCreatesRealCaAndTokenAndGraftsThemOntoConfig() {
        assertThat(caService.isCaInitialized()).isFalse();

        final var requested = CollectorsConfig.builder()
                .http(new IngestEndpointConfig("graylog.example.com", 14401))
                .build();

        final var result = initializer.initialize(requested);

        // A real CA hierarchy + token signing key were produced and grafted onto the returned config.
        assertThat(result.caCertId()).isNotBlank();
        assertThat(result.signingCertId()).isNotBlank();
        assertThat(result.otlpServerCertId()).isNotBlank();
        assertThat(result.tokenSigningKey()).isNotNull();
        assertThat(result.tokenSigningKey().fingerprint()).isNotBlank();

        // Caller-supplied fields are preserved.
        assertThat(result.http()).isEqualTo(requested.http());

        // initialize() builds the config but does not persist it — the caller saves (as CollectorsConfigResource
        // does). isCaInitialized() is config-backed, so it only flips to true once the caller has saved.
        assertThat(collectorsConfigService.get()).isEmpty();
        assertThat(caService.isCaInitialized()).isFalse();

        collectorsConfigService.save(result);

        assertThat(caService.isCaInitialized()).isTrue();
        // The grafted ids resolve to the actually-persisted certs.
        final var hierarchy = caService.loadHierarchy();
        assertThat(result.caCertId()).isEqualTo(hierarchy.caCert().id());
        assertThat(result.signingCertId()).isEqualTo(hierarchy.signingCert().id());
        assertThat(result.otlpServerCertId()).isEqualTo(hierarchy.otlpServerCert().id());
    }

    @Test
    void initializeFailsFastOnceConfigIsPersisted() {
        final var requested = CollectorsConfig.builder()
                .http(new IngestEndpointConfig("graylog.example.com", 14401))
                .build();

        // The resource persists the config initialize() returns; once it's saved the CA is "initialized".
        collectorsConfigService.save(initializer.initialize(requested));

        // A second bootstrap then fails fast via initializeCa()'s (config-backed) guard.
        assertThatThrownBy(() -> initializer.initialize(requested)).isInstanceOf(IllegalStateException.class);
    }
}
