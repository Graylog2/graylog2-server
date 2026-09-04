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
package org.graylog.collectors.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.collectors.config.GoDurationSerializer.toGoString;

class GoDurationSerializerTest {

    @Test
    void zero() {
        assertThat(toGoString(Duration.ZERO)).isEqualTo("0s");
    }

    @Test
    void hoursOnly() {
        assertThat(toGoString(Duration.ofHours(24))).isEqualTo("24h");
    }

    @Test
    void minutesOnly() {
        assertThat(toGoString(Duration.ofMinutes(5))).isEqualTo("5m");
    }

    @Test
    void secondsOnly() {
        assertThat(toGoString(Duration.ofSeconds(30))).isEqualTo("30s");
    }

    @Test
    void hoursAndMinutes() {
        assertThat(toGoString(Duration.ofHours(1).plusMinutes(30))).isEqualTo("1h30m");
    }

    @Test
    void hoursMinutesAndSeconds() {
        assertThat(toGoString(Duration.ofHours(1).plusMinutes(2).plusSeconds(3))).isEqualTo("1h2m3s");
    }

    @Test
    void millisecondsOnly() {
        assertThat(toGoString(Duration.ofMillis(500))).isEqualTo("500ms");
    }

    @Test
    void mixedSubSecondUnits() {
        assertThat(toGoString(Duration.ofNanos(1_500_000))).isEqualTo("1ms500us");
    }

    @Test
    void nanosecondsOnly() {
        assertThat(toGoString(Duration.ofNanos(42))).isEqualTo("42ns");
    }

    @Test
    void secondsAndMilliseconds() {
        assertThat(toGoString(Duration.ofSeconds(1).plusMillis(500))).isEqualTo("1s500ms");
    }

    @Test
    void allUnits() {
        final var d = Duration.ofHours(1).plusMinutes(2).plusSeconds(3)
                .plusMillis(4).plusNanos(5_006);
        assertThat(toGoString(d)).isEqualTo("1h2m3s4ms5us6ns");
    }

    @Test
    void negative() {
        assertThat(toGoString(Duration.ofSeconds(-30))).isEqualTo("-30s");
    }

    @Test
    void negativeComplex() {
        assertThat(toGoString(Duration.ofHours(-1).minusMinutes(30))).isEqualTo("-1h30m");
    }

    @Test
    void jacksonSerialization() throws Exception {
        record DurationHolder(@JsonSerialize(using = GoDurationSerializer.class) Duration value) {}

        final var json = new ObjectMapper().writeValueAsString(new DurationHolder(Duration.ofHours(1).plusSeconds(30)));

        assertThat(json).isEqualTo("{\"value\":\"1h30s\"}");
    }
}
