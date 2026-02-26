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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MacOSUnifiedLoggingRecordProcessorTest {

    private final MacOSUnifiedLoggingRecordProcessor processor = new MacOSUnifiedLoggingRecordProcessor();

    @Test
    void returnsEmptyMap() {
        final var logRecord = LogRecord.newBuilder()
                .addAttributes(KeyValue.newBuilder()
                        .setKey("some.attribute")
                        .setValue(AnyValue.newBuilder().setStringValue("some value")))
                .build();

        assertThat(processor.process(logRecord)).isEmpty();
    }
}
