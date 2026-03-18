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
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenSigningKeyTest {
    // Use MongoJackObjectMapperProvider to get a mapper configured for MongoDB serialization,
    // which enables database mode for EncryptedValue and uses the correct Date serialization.
    private final ObjectMapper objectMapper = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get()).get();

    @Test
    void serializationRoundtrip() throws Exception {
        final var createdAt = Instant.parse("2025-06-15T10:30:00.123Z");
        final var original = new TokenSigningKey(createEncryptedValue(), "SHA256:abc", createdAt);

        final var deserialized = serializeAndDeserialize(original);

        assertThat(deserialized.createdAt()).isEqualTo(createdAt);
        assertThat(deserialized.fingerprint()).isEqualTo(original.fingerprint());
        assertThat(deserialized.privateKey()).isEqualTo(original.privateKey());
    }

    /**
     * Serializes and deserializes via {@link TokenBuffer}, which simulates the MongoDB BSON roundtrip
     * path used by MongoJack without requiring a running MongoDB instance.
     */
    private TokenSigningKey serializeAndDeserialize(TokenSigningKey original) throws Exception {
        final var buffer = new TokenBuffer(objectMapper, false);
        objectMapper.writeValue(buffer, original);
        return objectMapper.readValue(buffer.asParser(), TokenSigningKey.class);
    }

    private EncryptedValue createEncryptedValue() {
        return EncryptedValue.builder()
                .value("2d043f9a7d5a5a7537d3e93c93c5dc40")
                .salt("c93c0263bfc3713d")
                .isKeepValue(false)
                .isDeleteValue(false)
                .build();
    }
}
