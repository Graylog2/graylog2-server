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

class FleetDTOTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void jsonRoundTrip() throws Exception {
        final var now = Instant.now();
        final var fleet = FleetDTO.builder()
                .id("fleet-id-1")
                .name("My Fleet")
                .description("A test fleet")
                .targetVersion("1.2.3")
                .createdAt(now)
                .updatedAt(now)
                .build();

        final var json = objectMapper.writeValueAsString(fleet);
        final var deserialized = objectMapper.readValue(json, FleetDTO.class);

        assertThat(deserialized).isEqualTo(fleet);
        assertThat(deserialized.name()).isEqualTo("My Fleet");
        assertThat(deserialized.description()).isEqualTo("A test fleet");
        assertThat(deserialized.targetVersion()).isEqualTo("1.2.3");
        assertThat(deserialized.createdAt()).isEqualTo(now);
        assertThat(deserialized.updatedAt()).isEqualTo(now);
    }

    @Test
    void nullableTargetVersion() {
        final var now = Instant.now();
        final var fleet = FleetDTO.builder()
                .name("My Fleet")
                .description("No version set")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(fleet.targetVersion()).isNull();
    }
}
