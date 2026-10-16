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
package org.graylog2.users;

import org.graylog2.plugin.validate.ConfigValidationException;
import org.junit.jupiter.api.Test;
import org.threeten.extra.PeriodDuration;

import java.time.Duration;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserConfigurationValidatorTest {
    private final UserConfigurationValidator validator = new UserConfigurationValidator();

    @Test
    void acceptsDefaults() {
        assertThatCode(() -> validator.validate(UserConfiguration.defaults())).doesNotThrowAnyException();
    }

    @Test
    void ignoresSessionTimeoutIntervalWhenDisabled() {
        assertThatCode(() -> validator.validate(config(false, Duration.ZERO, PeriodDuration.of(Period.ofDays(1)))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShortSessionTimeoutInterval() {
        assertThatThrownBy(() -> validator.validate(config(true, Duration.ZERO, PeriodDuration.of(Period.ofDays(1)))))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void rejectsTooShortTokenTtl() {
        assertThatThrownBy(() -> validator.validate(config(false, Duration.ofHours(1), PeriodDuration.of(Duration.ofHours(23)))))
                .isInstanceOf(ConfigValidationException.class);
    }

    private static UserConfiguration config(boolean enableTimeout, Duration timeout, PeriodDuration ttl) {
        return UserConfiguration.create(enableTimeout, timeout, false, true, ttl);
    }
}
