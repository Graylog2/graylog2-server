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
package org.graylog.collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectorOSTypeTest {

    @ParameterizedTest
    @CsvSource({
            "linux, LINUX",
            "darwin, MACOS",
            "unknown, UNKNOWN",
            "windows, WINDOWS"
    })
    void ofResolvesKnownOSNames(String osName, CollectorOSType expected) {
        assertThat(CollectorOSType.of(osName)).isEqualTo(expected);
    }

    @Test
    void ofReturnsUNKNOWNForUnknownOSName() {
        assertThat(CollectorOSType.of("freebsd")).isEqualTo(CollectorOSType.UNKNOWN);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void ofThrowsForBlankOrNullOSName(String osName) {
        assertThatThrownBy(() -> CollectorOSType.of(osName))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofIsCaseSensitive() {
        assertThat(CollectorOSType.of("Linux")).isEqualTo(CollectorOSType.UNKNOWN);
    }
}
