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
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceDTOTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(
                new NamedType(FileSourceConfig.class, FileSourceConfig.TYPE_NAME),
                new NamedType(JournaldSourceConfig.class, JournaldSourceConfig.TYPE_NAME),
                new NamedType(WindowsEventLogSourceConfig.class, WindowsEventLogSourceConfig.TYPE_NAME),
                new NamedType(TcpSourceConfig.class, TcpSourceConfig.TYPE_NAME),
                new NamedType(UdpSourceConfig.class, UdpSourceConfig.TYPE_NAME)
        );
    }

    @Test
    void jsonRoundTrip() throws Exception {
        final var original = SourceDTO.builder()
                .id("abc123")
                .fleetId("fleet-1")
                .name("Syslog file source")
                .description("Reads syslog")
                .enabled(true)
                .config(new FileSourceConfig(
                        List.of("/var/log/syslog"),
                        "tail",
                        new MultilineConfig("^\\d{4}-", true)
                ))
                .build();

        final var json = objectMapper.writeValueAsString(original);
        final var deserialized = objectMapper.readValue(json, SourceDTO.class);

        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.fleetId()).isEqualTo("fleet-1");
        assertThat(deserialized.name()).isEqualTo("Syslog file source");
        assertThat(deserialized.description()).isEqualTo("Reads syslog");
        assertThat(deserialized.enabled()).isTrue();
        assertThat(deserialized.config()).isInstanceOf(FileSourceConfig.class);
    }

    @Test
    void typeIsDerivedFromConfig() {
        final var source = SourceDTO.builder()
                .fleetId("fleet-1")
                .name("TCP source")
                .description("TCP listener")
                .config(new TcpSourceConfig("0.0.0.0", 5140))
                .build();

        assertThat(source.config().type()).isEqualTo("tcp");
    }

    @Test
    void defaultEnabledIsTrue() {
        final var source = SourceDTO.builder()
                .fleetId("fleet-1")
                .name("Test source")
                .description("Testing defaults")
                .config(new FileSourceConfig(List.of("/var/log/test"), "tail", null))
                .build();

        assertThat(source.enabled()).isTrue();
    }
}
