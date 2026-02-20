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
package org.graylog2.opamp;

import opamp.proto.Opamp;
import opamp.proto.Opamp.ConnectionSettingsOffers;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog.security.pki.CertificateEntry;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.encryption.EncryptedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for OTLP connection settings construction in {@link OpAmpService}.
 */
class OpAmpServiceOtlpSettingsTest {

    private OpAmpCaService opAmpCaService;
    private ClusterConfigService clusterConfigService;

    private static final String CA_PEM = "-----BEGIN CERTIFICATE-----\nfake-ca-pem\n-----END CERTIFICATE-----";
    private static final String CLUSTER_ID = "2209F727-F7E1-4123-9386-94FE3B354A07";

    @BeforeEach
    void setUp() {
        opAmpCaService = mock(OpAmpCaService.class);
        clusterConfigService = mock(ClusterConfigService.class);

        final var caCert = new CertificateEntry(
                "ca-id", "sha256:abc", EncryptedValue.createUnset(),
                CA_PEM, List.of(), null, null,
                Instant.now(), Instant.now().plusSeconds(86400), Instant.now());
        when(opAmpCaService.getOpAmpCa()).thenReturn(caCert);
        when(clusterConfigService.get(ClusterId.class)).thenReturn(ClusterId.create(CLUSTER_ID));
    }

    @Test
    void usesTlsCaPemContentsNotCertificateCaCert() {
        final var config = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "otlp.example.com", 14401, "input-1"),
                new IngestEndpointConfig(false, "otlp.example.com", 14402, null)
        );

        final ConnectionSettingsOffers.Builder builder = ConnectionSettingsOffers.newBuilder();
        OpAmpService.buildOtlpConnectionSettings(builder, config, opAmpCaService, clusterConfigService);

        final Opamp.OtherConnectionSettings httpSettings = builder.getOtherConnectionsOrThrow("otlp-http");

        // Trust anchor must be in tls.ca_pem_contents
        assertThat(httpSettings.getTls().getCaPemContents()).isEqualTo(CA_PEM);
        // Must NOT be in certificate.ca_cert
        assertThat(httpSettings.getCertificate().getCaCert().toStringUtf8()).isEmpty();
    }

    @Test
    void includesServerNameInOtherSettings() {
        final var config = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "otlp.example.com", 14401, "input-1"),
                new IngestEndpointConfig(false, "otlp.example.com", 14402, null)
        );

        final ConnectionSettingsOffers.Builder builder = ConnectionSettingsOffers.newBuilder();
        OpAmpService.buildOtlpConnectionSettings(builder, config, opAmpCaService, clusterConfigService);

        final Opamp.OtherConnectionSettings httpSettings = builder.getOtherConnectionsOrThrow("otlp-http");
        assertThat(httpSettings.getOtherSettingsOrThrow("server_name")).isEqualTo(CLUSTER_ID);
    }

    @Test
    void setsCorrectDestinationEndpoint() {
        final var config = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "otlp.example.com", 14401, "input-1"),
                new IngestEndpointConfig(true, "grpc.example.com", 14402, "input-2")
        );

        final ConnectionSettingsOffers.Builder builder = ConnectionSettingsOffers.newBuilder();
        OpAmpService.buildOtlpConnectionSettings(builder, config, opAmpCaService, clusterConfigService);

        assertThat(builder.getOtherConnectionsOrThrow("otlp-http").getDestinationEndpoint())
                .isEqualTo("https://otlp.example.com:14401");
        assertThat(builder.getOtherConnectionsOrThrow("otlp-grpc").getDestinationEndpoint())
                .isEqualTo("https://grpc.example.com:14402");
    }

    @Test
    void skipsDisabledProtocols() {
        final var config = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(false, "host", 14401, null),
                new IngestEndpointConfig(false, "host", 14402, null)
        );

        final ConnectionSettingsOffers.Builder builder = ConnectionSettingsOffers.newBuilder();
        OpAmpService.buildOtlpConnectionSettings(builder, config, opAmpCaService, clusterConfigService);

        assertThat(builder.getOtherConnectionsMap()).isEmpty();
    }

    @Test
    void omitsServerNameWhenClusterIdIsNull() {
        when(clusterConfigService.get(ClusterId.class)).thenReturn(null);

        final var config = new CollectorsConfig(
                "ca-id", "token-id", "otlp-id",
                new IngestEndpointConfig(true, "host", 14401, "input-1"),
                new IngestEndpointConfig(false, "host", 14402, null)
        );

        final ConnectionSettingsOffers.Builder builder = ConnectionSettingsOffers.newBuilder();
        OpAmpService.buildOtlpConnectionSettings(builder, config, opAmpCaService, clusterConfigService);

        final Opamp.OtherConnectionSettings httpSettings = builder.getOtherConnectionsOrThrow("otlp-http");
        assertThat(httpSettings.getOtherSettingsMap()).doesNotContainKey("server_name");
        // Trust anchor should still be set
        assertThat(httpSettings.getTls().getCaPemContents()).isEqualTo(CA_PEM);
    }
}
