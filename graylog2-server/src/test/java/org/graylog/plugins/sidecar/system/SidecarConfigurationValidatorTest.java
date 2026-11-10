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
package org.graylog.plugins.sidecar.system;

import org.graylog2.plugin.validate.ConfigValidationException;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SidecarConfigurationValidatorTest {
    private final SidecarConfigurationValidator validator = new SidecarConfigurationValidator();

    @Test
    void acceptsDefaults() {
        assertThatCode(() -> validator.validate(SidecarConfiguration.defaults())).doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShortInactiveThreshold() {
        final var config = SidecarConfiguration.defaults().toBuilder()
                .sidecarInactiveThreshold(Period.millis(500))
                .sidecarUpdateInterval(Period.millis(100))
                .build();

        assertThatThrownBy(() -> validator.validate(config)).isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void rejectsTooShortExpirationThreshold() {
        final var config = SidecarConfiguration.defaults().toBuilder()
                .sidecarExpirationThreshold(Period.seconds(30))
                .build();

        assertThatThrownBy(() -> validator.validate(config)).isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void rejectsUpdateIntervalNotShorterThanInactiveThreshold() {
        final var config = SidecarConfiguration.defaults().toBuilder()
                .sidecarUpdateInterval(Period.minutes(1))
                .build();

        assertThatThrownBy(() -> validator.validate(config))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("Update interval");
    }
}
