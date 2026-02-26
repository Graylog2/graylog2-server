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
package org.graylog.collectors.input.processor;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.collectors.config.OtelAttributes;
import org.graylog.schema.EventFields;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilelogRecordProcessorTest {

    private final FilelogRecordProcessor processor = new FilelogRecordProcessor();

    @Test
    void mapsSupportedAttributes() {
        final var logRecord = LogRecord.newBuilder()
                .addAttributes(stringAttribute(OtelAttributes.COLLECTOR_RECEIVER_TYPE, "filelog"))
                .addAttributes(stringAttribute("log.file.name", "graylog.log"))
                .addAttributes(stringAttribute("log.file.path", "/var/log/graylog/graylog.log"))
                .addAttributes(stringAttribute("log.file.owner.name", "graylog"))
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                EventFields.EVENT_LOG_NAME, "graylog.log",
                EventFields.EVENT_LOG_PATH, "/var/log/graylog/graylog.log"
        ));
    }

    @Test
    void returnsEmptyMapWhenNoSupportedAttributesExist() {
        final var logRecord = LogRecord.newBuilder()
                .addAttributes(stringAttribute("log.file.owner.name", "graylog"))
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).isEmpty();
    }

    private static KeyValue stringAttribute(String key, String value) {
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setStringValue(value).build())
                .build();
    }
}
