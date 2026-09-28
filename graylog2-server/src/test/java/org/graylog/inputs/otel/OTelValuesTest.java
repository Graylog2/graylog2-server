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
package org.graylog.inputs.otel;

import io.opentelemetry.proto.common.v1.AnyValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OTelValuesTest {

    @Test
    void unsignedAsStringRendersBothEncodings() {
        // Below 2^63 the sender uses an OTLP integer.
        assertThat(OTelValues.unsignedAsString(intValue(0))).isEqualTo("0");
        assertThat(OTelValues.unsignedAsString(intValue(45473881108119556L))).isEqualTo("45473881108119556");
        assertThat(OTelValues.unsignedAsString(intValue(Long.MAX_VALUE))).isEqualTo("9223372036854775807");

        // At or above 2^63 it cannot, and sends the decimal string instead.
        assertThat(OTelValues.unsignedAsString(strValue("9223372036854775808"))).isEqualTo("9223372036854775808");
        assertThat(OTelValues.unsignedAsString(strValue("18446744073709551615"))).isEqualTo("18446744073709551615");
    }

    @Test
    void unsignedAsStringDoesNotWrapNegative() {
        // A sender that ignored the mapping and wrapped the value must not be silently re-wrapped:
        // the bit pattern is read back as the unsigned value it stands for.
        assertThat(OTelValues.unsignedAsString(intValue(-1))).isEqualTo("18446744073709551615");
    }

    @Test
    void unsignedAsStringRejectsAbsentAndNonNumeric() {
        assertThat(OTelValues.unsignedAsString(AnyValue.getDefaultInstance())).isNull();
        assertThat(OTelValues.unsignedAsString(strValue(""))).isNull();
        assertThat(OTelValues.unsignedAsString(AnyValue.newBuilder().setBoolValue(true).build())).isNull();
    }

    @Test
    void asLongAcceptsBothEncodings() {
        assertThat(OTelValues.asLong(intValue(7371))).isEqualTo(7371L);
        assertThat(OTelValues.asLong(strValue("7371"))).isEqualTo(7371L);
        assertThat(OTelValues.asLong(strValue("-1"))).isEqualTo(-1L);
    }

    @Test
    void asLongRejectsWhatDoesNotFitASignedLong() {
        assertThat(OTelValues.asLong(strValue("18446744073709551615"))).isNull();
        assertThat(OTelValues.asLong(strValue("not a number"))).isNull();
        assertThat(OTelValues.asLong(strValue(""))).isNull();
        assertThat(OTelValues.asLong(AnyValue.getDefaultInstance())).isNull();
    }

    private static AnyValue intValue(long value) {
        return AnyValue.newBuilder().setIntValue(value).build();
    }

    private static AnyValue strValue(String value) {
        return AnyValue.newBuilder().setStringValue(value).build();
    }
}
