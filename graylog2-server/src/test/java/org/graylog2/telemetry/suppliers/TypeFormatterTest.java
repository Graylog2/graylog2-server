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
