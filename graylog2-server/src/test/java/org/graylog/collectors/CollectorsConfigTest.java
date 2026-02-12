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

import static org.assertj.core.api.Assertions.assertThat;

class CollectorsConfigTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void serializesAndDeserializes() throws Exception {
        final var http = new IngestEndpointConfig(true, "graylog.example.com", 14401, "input-1");
        final var grpc = new IngestEndpointConfig(false, "graylog.example.com", 14402, null);
        final var config = new CollectorsConfig("ca-id", "token-id", "otlp-id", http, grpc);

        final var json = objectMapper.writeValueAsString(config);
        final var deserialized = objectMapper.readValue(json, CollectorsConfig.class);

        assertThat(deserialized).isEqualTo(config);
        assertThat(json).contains("\"opamp_ca_id\"");
        assertThat(json).contains("\"token_signing_cert_id\"");
        assertThat(json).contains("\"otlp_server_cert_id\"");
    }

    @Test
    void ingestEndpointConfigWithNullInputId() throws Exception {
        final var endpoint = new IngestEndpointConfig(true, "host.example.com", 14401, null);
        final var json = objectMapper.writeValueAsString(endpoint);
        final var deserialized = objectMapper.readValue(json, IngestEndpointConfig.class);

        assertThat(deserialized.inputId()).isNull();
        assertThat(deserialized.enabled()).isTrue();
        assertThat(deserialized.hostname()).isEqualTo("host.example.com");
        assertThat(deserialized.port()).isEqualTo(14401);
    }

    @Test
    void nullableCertIds() throws Exception {
        final var http = new IngestEndpointConfig(true, "host", 14401, null);
        final var grpc = new IngestEndpointConfig(false, "host", 14402, null);
        final var config = new CollectorsConfig(null, null, null, http, grpc);

        final var json = objectMapper.writeValueAsString(config);
        final var deserialized = objectMapper.readValue(json, CollectorsConfig.class);

        assertThat(deserialized).isEqualTo(config);
        assertThat(deserialized.opampCaId()).isNull();
        assertThat(deserialized.tokenSigningCertId()).isNull();
        assertThat(deserialized.otlpServerCertId()).isNull();
    }
}
