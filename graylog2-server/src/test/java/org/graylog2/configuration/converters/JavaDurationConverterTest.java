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
package org.graylog2.configuration.converters;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JavaDurationConverterTest {

    private final JavaDurationConverter converter;

    JavaDurationConverterTest() {
        this.converter = new JavaDurationConverter();
    }

    @Test
    public void convertFrom() {
        assertThat(converter.convertFrom("10ms")).isEqualTo(Duration.ofMillis(10));
        assertThat(converter.convertFrom("10s")).isEqualTo(Duration.ofSeconds(10));
        assertThat(converter.convertFrom("PT0.01S")).isEqualTo(Duration.ofMillis(10));
        assertThat(converter.convertFrom("PT10S")).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    public void convertTo() {
        assertThat(converter.convertTo(Duration.ofMillis(10))).isEqualTo("PT0.01S");
        assertThat(converter.convertTo(Duration.ofSeconds(10))).isEqualTo("PT10S");
        assertThat(converter.convertTo(Duration.ofSeconds(70))).isEqualTo("PT1M10S");
    }

    @Test
    public void convertBackAndForth() {
        assertThat(converter.convertFrom(converter.convertTo(Duration.ofSeconds(70)))).isEqualTo(Duration.ofSeconds(70));
        assertThat(converter.convertTo(converter.convertFrom("70s"))).isEqualTo("PT1M10S");
    }

    @Test
    public void convertComplex() {
        assertThat(converter.convertTo(Duration.parse("PT1h5m"))).isEqualTo("PT1H5M");
        assertThat(converter.convertTo(Duration.parse("PT5m3s"))).isEqualTo("PT5M3S");
        assertThat(converter.convertTo(Duration.parse("PT0M0.25S"))).isEqualTo("PT0.25S");

        assertThat(converter.convertTo(Duration.parse("P1DT2S"))).isEqualTo("PT24H2S");
    }

}
