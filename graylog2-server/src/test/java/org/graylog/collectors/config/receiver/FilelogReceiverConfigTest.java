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
package org.graylog.collectors.config.receiver;

import org.graylog.collectors.CollectorOSType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilelogReceiverConfigTest {

    private static FilelogReceiverConfig.Builder minimalBuilder() {
        return FilelogReceiverConfig.builder("abc123")
                .include(List.of("/var/log/example.log"));
    }

    @Test
    void windowsDisablesFileOwnerOptions() {
        // The file_log receiver fails collector startup on Windows when the file owner options are enabled.
        final var config = minimalBuilder().build(CollectorOSType.WINDOWS);

        assertThat(config.includeFileOwnerName()).isFalse();
        assertThat(config.includeFileOwnerGroupName()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CollectorOSType.class, mode = EnumSource.Mode.EXCLUDE, names = {"WINDOWS", "UNKNOWN"})
    void otherOsTypesKeepFileOwnerOptionsEnabled(CollectorOSType osType) {
        final var config = minimalBuilder().build(osType);

        assertThat(config.includeFileOwnerName()).isTrue();
        assertThat(config.includeFileOwnerGroupName()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CollectorOSType.class, mode = EnumSource.Mode.EXCLUDE, names = {"WINDOWS", "UNKNOWN"})
    void explicitlyDisabledFileOwnerOptionsAreNotOverridden(CollectorOSType osType) {
        final var config = minimalBuilder()
                .includeFileOwnerName(false)
                .includeFileOwnerGroupName(false)
                .build(osType);

        assertThat(config.includeFileOwnerName()).isFalse();
        assertThat(config.includeFileOwnerGroupName()).isFalse();
    }
}
