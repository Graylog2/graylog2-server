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
package org.graylog.collectors.config.receiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.collectors.CollectorOSType;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MacOSUnifiedLoggingReceiverConfigTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperProvider().get();
    }

    @Test
    void defaultsAndSerialization() throws Exception {
        final var config = MacOSUnifiedLoggingReceiverConfig.builder("source-1").build();

        assertThat(config.type()).isEqualTo("macos_unified_logging");
        assertThat(config.name()).isEqualTo("macos_unified_logging/source-1");
        assertThat(config.osSupport()).containsExactly(CollectorOSType.MACOS);

        final var tree = objectMapper.readTree(objectMapper.writeValueAsString(config));
        assertThat(tree.get("storage").asText()).isEqualTo("file_storage/default");
        assertThat(tree.get("format").asText()).isEqualTo("ndjson");
        assertThat(tree.get("predicate").asText()).contains("subsystem");
        assertThat(tree.has("max_poll_interval")).isTrue();
        assertThat(tree.has("max_log_age")).isTrue();
        // @JsonIgnore fields must not leak into the rendered receiver block
        assertThat(tree.has("type")).isFalse();
        assertThat(tree.has("name")).isFalse();
    }

    @Test
    void overridesPredicate() throws Exception {
        final var config = MacOSUnifiedLoggingReceiverConfig.builder("s")
                .predicate("subsystem == 'com.apple.securityd'")
                .build();
        final var tree = objectMapper.readTree(objectMapper.writeValueAsString(config));
        assertThat(tree.get("predicate").asText()).isEqualTo("subsystem == 'com.apple.securityd'");
    }
}
