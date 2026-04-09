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
package org.graylog.collectors.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentTokenDTOTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void serializesAndDeserializes() throws Exception {
        final var now = Instant.now();
        final var creator = new EnrollmentTokenCreator("user-1", "alice");
        final var dto = new EnrollmentTokenDTO(
                "token-id-1",
                "token-name-1",
                "jti-abc",
                "kid-abc",
                "fleet-1",
                creator,
                now,
                now.plusSeconds(3600),
                5,
                now.minusSeconds(60)
        );

        final var json = objectMapper.writeValueAsString(dto);
        final var deserialized = objectMapper.readValue(json, EnrollmentTokenDTO.class);

        assertThat(deserialized).isEqualTo(dto);
        assertThat(deserialized.id()).isEqualTo("token-id-1");
        assertThat(deserialized.name()).isEqualTo("token-name-1");
        assertThat(deserialized.jti()).isEqualTo("jti-abc");
        assertThat(deserialized.kid()).isEqualTo("kid-abc");
        assertThat(deserialized.fleetId()).isEqualTo("fleet-1");
        assertThat(deserialized.createdBy().userId()).isEqualTo("user-1");
        assertThat(deserialized.createdBy().username()).isEqualTo("alice");
        assertThat(deserialized.createdAt()).isEqualTo(now);
        assertThat(deserialized.expiresAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(deserialized.usageCount()).isEqualTo(5);
        assertThat(deserialized.lastUsedAt()).isEqualTo(now.minusSeconds(60));
    }

    @Test
    void serializesWithNullExpiresAt() throws Exception {
        final var now = Instant.now();
        final var creator = new EnrollmentTokenCreator("user-2", "bob");
        final var dto = new EnrollmentTokenDTO(
                "token-id-2",
                "token-name-2",
                "jti-xyz",
                "kid-xyz",
                "fleet-2",
                creator,
                now,
                null,
                0,
                null
        );

        final var json = objectMapper.writeValueAsString(dto);
        final var deserialized = objectMapper.readValue(json, EnrollmentTokenDTO.class);

        assertThat(deserialized).isEqualTo(dto);
        assertThat(deserialized.expiresAt()).isNull();
        assertThat(deserialized.lastUsedAt()).isNull();
        assertThat(deserialized.usageCount()).isZero();
    }

    @Test
    void jsonFieldNames() throws Exception {
        final var now = Instant.now();
        final var creator = new EnrollmentTokenCreator("user-3", "carol");
        final var dto = new EnrollmentTokenDTO(
                "token-id-3",
                "token-name-3",
                "jti-def",
                "kid-def",
                "fleet-3",
                creator,
                now,
                null,
                2,
                now.minusSeconds(30)
        );

        final var json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"jti\"");
        assertThat(json).contains("\"kid\"");
        assertThat(json).contains("\"name\"");
        assertThat(json).contains("\"fleet_id\"");
        assertThat(json).contains("\"created_by\"");
        assertThat(json).contains("\"created_at\"");
        assertThat(json).contains("\"usage_count\"");
        assertThat(json).contains("\"last_used_at\"");
    }
}
