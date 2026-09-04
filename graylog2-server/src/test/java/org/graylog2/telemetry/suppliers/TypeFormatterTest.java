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
package org.graylog2.telemetry.suppliers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeFormatterTest {
    @ParameterizedTest
    @CsvSource({
            "org.graylog.plugins.beats.Beats2Input, beats_2_input",
            "org.graylog2.inputs.beats.kafka.BeatsKafkaInput, beats_kafka_input",
            "org.graylog2.inputs.gelf.http.GELFHttpInput, gelf_http_input",
            "org.graylog2.inputs.syslog.tcp.SyslogTCPInput, syslog_tcp_input",
            "org.graylog.aws.inputs.TestInput, test_input",
            "org.test.testInput, org.test.testInput",
    })
    void shouldFormatType(String type, String expected) {
        assertEquals(expected, TypeFormatter.format(type));
    }
}
