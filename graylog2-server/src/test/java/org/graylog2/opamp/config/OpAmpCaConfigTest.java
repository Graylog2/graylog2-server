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
                "enrollment-ca-object-id-123",
                "token-signing-cert-object-id-456"
        );

        final String json = objectMapper.writeValueAsString(config);

        assertThat(json).contains("\"enrollment_ca_id\":\"enrollment-ca-object-id-123\"");
        assertThat(json).contains("\"token_signing_cert_id\":\"token-signing-cert-object-id-456\"");
    }

    @Test
    void testJsonDeserialization() throws JsonProcessingException {
        final String json = """
                {
                    "enrollment_ca_id": "enrollment-ca-object-id-123",
                    "token_signing_cert_id": "token-signing-cert-object-id-456"
                }
                """;

        final OpAmpCaConfig config = objectMapper.readValue(json, OpAmpCaConfig.class);

        assertThat(config.enrollmentCaId()).isEqualTo("enrollment-ca-object-id-123");
        assertThat(config.tokenSigningCertId()).isEqualTo("token-signing-cert-object-id-456");
    }

    @Test
    void testJsonRoundtrip() throws JsonProcessingException {
        final OpAmpCaConfig original = new OpAmpCaConfig(
                "test-enrollment-ca-id",
                "test-token-signing-id"
        );

        final String json = objectMapper.writeValueAsString(original);
        final OpAmpCaConfig deserialized = objectMapper.readValue(json, OpAmpCaConfig.class);

        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void testNullValues() throws JsonProcessingException {
        final OpAmpCaConfig config = new OpAmpCaConfig(null, null);

        final String json = objectMapper.writeValueAsString(config);
        final OpAmpCaConfig deserialized = objectMapper.readValue(json, OpAmpCaConfig.class);

        assertThat(deserialized.enrollmentCaId()).isNull();
        assertThat(deserialized.tokenSigningCertId()).isNull();
    }
}
