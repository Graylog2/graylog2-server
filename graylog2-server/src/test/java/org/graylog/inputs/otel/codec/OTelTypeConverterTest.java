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
package org.graylog.inputs.otel.codec;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OTelTypeConverterTest {

    private OTelTypeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new OTelTypeConverter(new ObjectMapperProvider().get());
    }

    @Test
    void stringValue() {
        final var value = AnyValue.newBuilder().setStringValue("hello").build();
        assertThat(converter.toString(value, "test")).hasValue("hello");
    }

    @Test
    void boolValue() {
        final var value = AnyValue.newBuilder().setBoolValue(true).build();
        assertThat(converter.toString(value, "test")).hasValue("true");
    }

    @Test
    void intValue() {
        final var value = AnyValue.newBuilder().setIntValue(42).build();
        assertThat(converter.toString(value, "test")).hasValue("42");
    }

    @Test
    void doubleValue() {
        final var value = AnyValue.newBuilder().setDoubleValue(3.14).build();
        assertThat(converter.toString(value, "test")).hasValue("3.14");
    }

    @Test
    void bytesValue() {
        final var value = AnyValue.newBuilder()
                .setBytesValue(ByteString.copyFrom(new byte[]{1, 2, 3}))
                .build();
        assertThat(converter.toString(value, "test")).hasValue("AQID");
    }

    @Test
    void arrayValue() {
        final var value = AnyValue.newBuilder()
                .setArrayValue(ArrayValue.newBuilder()
                        .addValues(AnyValue.newBuilder().setStringValue("a"))
                        .addValues(AnyValue.newBuilder().setStringValue("b")))
                .build();
        assertThat(converter.toString(value, "test")).hasValue("[\"a\",\"b\"]");
    }

    @Test
    void kvListValue() {
        final var value = AnyValue.newBuilder()
                .setKvlistValue(KeyValueList.newBuilder()
                        .addValues(KeyValue.newBuilder()
                                .setKey("k")
                                .setValue(AnyValue.newBuilder().setStringValue("v"))))
                .build();
        assertThat(converter.toString(value, "test")).hasValue("{\"k\":\"v\"}");
    }

    @Test
    void emptyValue() {
        final var value = AnyValue.getDefaultInstance();
        assertThat(converter.toString(value, "test")).isEmpty();
    }
}
