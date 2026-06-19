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
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorsConfigTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void serializesAndDeserializes() throws Exception {
        final var http = new IngestEndpointConfig("graylog.example.com", 14401);
        final var config = CollectorsConfig.builder()
                .caCertId("ca-cert-id")
                .signingCertId("signing-cert-id")
                .otlpServerCertId("otlp-server-id")
                .http(http)
                .build();

        final var json = objectMapper.writeValueAsString(config);
        final var deserialized = objectMapper.readValue(json, CollectorsConfig.class);

        assertThat(deserialized).isEqualTo(config);
        assertThat(json).contains("\"ca_cert_id\"");
        assertThat(json).contains("\"signing_cert_id\"");
        assertThat(json).contains("\"token_signing_key\"");
        assertThat(json).contains("\"otlp_server_cert_id\"");
    }

    @Test
    void nullableCertIds() throws Exception {
        final var config = CollectorsConfig.createDefaultBuilder("host")
                .caCertId(null)
                .signingCertId(null)
                .otlpServerCertId(null)
                .tokenSigningKey(null)
                .build();

        final var json = objectMapper.writeValueAsString(config);
        final var deserialized = objectMapper.readValue(json, CollectorsConfig.class);

        assertThat(deserialized).isEqualTo(config);
        assertThat(deserialized.caCertId()).isNull();
        assertThat(deserialized.signingCertId()).isNull();
        assertThat(deserialized.otlpServerCertId()).isNull();
    }

    @Test
    void serializesAndDeserializesWithThresholds() throws Exception {
        final var config = CollectorsConfig.createDefaultBuilder("host")
                .collectorOfflineThreshold(Duration.ofMinutes(10))
                .collectorDefaultVisibilityThreshold(Duration.ofHours(12))
                .collectorExpirationThreshold(Duration.ofDays(3))
                .build();

        final var json = objectMapper.writeValueAsString(config);
        final var deserialized = objectMapper.readValue(json, CollectorsConfig.class);

        assertThat(deserialized).isEqualTo(config);
        assertThat(deserialized.collectorOfflineThreshold()).isEqualTo(Duration.ofMinutes(10));
        assertThat(deserialized.collectorDefaultVisibilityThreshold()).isEqualTo(Duration.ofHours(12));
        assertThat(deserialized.collectorExpirationThreshold()).isEqualTo(Duration.ofDays(3));
    }
}
