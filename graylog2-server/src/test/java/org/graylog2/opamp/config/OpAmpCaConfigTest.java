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
package org.graylog2.opamp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpAmpCaConfigTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperProvider().get();
    }

    @Test
    void testJsonSerialization() throws JsonProcessingException {
        final OpAmpCaConfig config = new OpAmpCaConfig(
                "opamp-ca-object-id-123",
                "token-signing-cert-object-id-456",
                "otlp-server-cert-object-id-789"
        );

        final String json = objectMapper.writeValueAsString(config);

        assertThat(json).contains("\"opamp_ca_id\":\"opamp-ca-object-id-123\"");
        assertThat(json).contains("\"token_signing_cert_id\":\"token-signing-cert-object-id-456\"");
        assertThat(json).contains("\"otlp_server_cert_id\":\"otlp-server-cert-object-id-789\"");
    }

    @Test
    void testJsonDeserialization() throws JsonProcessingException {
        final String json = """
                {
                    "opamp_ca_id": "opamp-ca-object-id-123",
                    "token_signing_cert_id": "token-signing-cert-object-id-456",
                    "otlp_server_cert_id": "otlp-server-cert-object-id-789"
                }
                """;

        final OpAmpCaConfig config = objectMapper.readValue(json, OpAmpCaConfig.class);

        assertThat(config.opampCaId()).isEqualTo("opamp-ca-object-id-123");
        assertThat(config.tokenSigningCertId()).isEqualTo("token-signing-cert-object-id-456");
        assertThat(config.otlpServerCertId()).isEqualTo("otlp-server-cert-object-id-789");
    }

    @Test
    void testJsonRoundtrip() throws JsonProcessingException {
        final OpAmpCaConfig original = new OpAmpCaConfig(
                "test-opamp-ca-id",
                "test-token-signing-id",
                "test-otlp-server-cert-id"
        );

        final String json = objectMapper.writeValueAsString(original);
        final OpAmpCaConfig deserialized = objectMapper.readValue(json, OpAmpCaConfig.class);

        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void testNullValues() throws JsonProcessingException {
        final OpAmpCaConfig config = new OpAmpCaConfig(null, null, null);

        final String json = objectMapper.writeValueAsString(config);
        final OpAmpCaConfig deserialized = objectMapper.readValue(json, OpAmpCaConfig.class);

        assertThat(deserialized.opampCaId()).isNull();
        assertThat(deserialized.tokenSigningCertId()).isNull();
        assertThat(deserialized.otlpServerCertId()).isNull();
    }
}
