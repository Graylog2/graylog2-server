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
package org.graylog2.security.encryption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedValueSerializerTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        final EncryptedValueSerializer serializer = new EncryptedValueSerializer();
        final SimpleModule module = new SimpleModule("Test").addSerializer(EncryptedValue.class, serializer);

        this.objectMapper = new ObjectMapper().registerModule(module);
    }

    @Test
    void serialize() throws Exception {
        final EncryptedValue value = EncryptedValue.builder()
                .value("2d043f9a7d5a5a7537d3e93c93c5dc40")
                .salt("c93c0263bfc3713d")
                .isDeleteValue(false)
                .isKeepValue(false)
                .build();

        final String jsonString = objectMapper.writeValueAsString(value);

        final JsonNode node = objectMapper.readValue(jsonString, JsonNode.class);

        assertThat(node.path("is_set").isBoolean()).isTrue();
        assertThat(node.path("is_set").asBoolean()).isTrue();
    }

    @Test
    void serializeUnset() throws Exception {
        final EncryptedValue value = EncryptedValue.createUnset();

        final String jsonString = objectMapper.writeValueAsString(value);

        final JsonNode node = objectMapper.readValue(jsonString, JsonNode.class);

        assertThat(node.path("is_set").isBoolean()).isTrue();
        assertThat(node.path("is_set").asBoolean()).isFalse();
    }

    @Test
    void serializeForDatabase() throws Exception {
        EncryptedValueMapperConfig.enableDatabase(objectMapper);

        final EncryptedValue value = EncryptedValue.builder()
                .value("2d043f9a7d5a5a7537d3e93c93c5dc40")
                .salt("c93c0263bfc3713d")
                .isDeleteValue(false)
                .isKeepValue(false)
                .build();

        final String jsonString = objectMapper.writeValueAsString(value);

        final JsonNode node = objectMapper.readValue(jsonString, JsonNode.class);

        assertThat(node.path("encrypted_value").asText()).isEqualTo(value.value());
        assertThat(node.path("salt").asText()).isEqualTo(value.salt());
    }

    @Test
    void serializeUnsetForDatabase() throws Exception {
        EncryptedValueMapperConfig.enableDatabase(objectMapper);

        final EncryptedValue value = EncryptedValue.createUnset();

        final String jsonString = objectMapper.writeValueAsString(value);

        final JsonNode node = objectMapper.readValue(jsonString, JsonNode.class);

        assertThat(node.path("encrypted_value").isMissingNode()).isFalse();
        assertThat(node.path("encrypted_value").asText()).isEmpty();
        assertThat(node.path("salt").isMissingNode()).isFalse();
        assertThat(node.path("salt").asText()).isEmpty();
    }
}
