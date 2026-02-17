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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceConfigTest {
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
    void deserializeFileSource() throws Exception {
        final var json = """
                {
                    "type": "file",
                    "paths": ["/var/log/syslog", "/var/log/auth.log"],
                    "read_mode": "tail",
                    "multiline": {
                        "pattern": "^\\\\d{4}-",
                        "negate": true
                    }
                }
                """;

        final var config = objectMapper.readValue(json, SourceConfig.class);

        assertThat(config).isInstanceOf(FileSourceConfig.class);
        final var fileConfig = (FileSourceConfig) config;
        assertThat(fileConfig.type()).isEqualTo("file");
        assertThat(fileConfig.paths()).containsExactly("/var/log/syslog", "/var/log/auth.log");
        assertThat(fileConfig.readMode()).isEqualTo("tail");
        assertThat(fileConfig.multiline()).isNotNull();
        assertThat(fileConfig.multiline().pattern()).isEqualTo("^\\d{4}-");
        assertThat(fileConfig.multiline().negate()).isTrue();
    }

    @Test
    void deserializeTcpSource() throws Exception {
        final var json = """
                {
                    "type": "tcp",
                    "bind_address": "0.0.0.0",
                    "port": 5140
                }
                """;

        final var config = objectMapper.readValue(json, SourceConfig.class);

        assertThat(config).isInstanceOf(TcpSourceConfig.class);
        final var tcpConfig = (TcpSourceConfig) config;
        assertThat(tcpConfig.type()).isEqualTo("tcp");
        assertThat(tcpConfig.bindAddress()).isEqualTo("0.0.0.0");
        assertThat(tcpConfig.port()).isEqualTo(5140);
    }

    @Test
    void unknownTypeFallsBackToUnknownSourceConfig() throws Exception {
        final var json = """
                {
                    "type": "custom_enterprise_type",
                    "some_field": "some_value",
                    "nested": {"key": 42}
                }
                """;

        final var config = objectMapper.readValue(json, SourceConfig.class);

        assertThat(config).isInstanceOf(UnknownSourceConfig.class);
        assertThat(config.type()).isEqualTo("custom_enterprise_type");
        final var unknown = (UnknownSourceConfig) config;
        assertThat(unknown.additionalProperties()).containsEntry("some_field", "some_value");
    }

    @Test
    void fileSourceValidation() {
        final var config = new FileSourceConfig(List.of(), "tail", null);
        assertThatThrownBy(config::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tcpSourcePortValidation() {
        final var config = new TcpSourceConfig("0.0.0.0", 0);
        assertThatThrownBy(config::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jsonRoundTrip() throws Exception {
        final var original = new FileSourceConfig(
                List.of("/var/log/syslog"),
                "tail",
                new MultilineConfig("^\\d{4}-", true)
        );

        final var json = objectMapper.writeValueAsString(original);
        final var deserialized = objectMapper.readValue(json, SourceConfig.class);

        assertThat(deserialized).isEqualTo(original);
    }
}
