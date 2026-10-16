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

import org.graylog2.plugin.validate.ClusterConfigValidator;
import org.graylog2.plugin.validate.ConfigValidationException;

import java.time.Duration;
import java.time.LocalDateTime;

public class UserConfigurationValidator implements ClusterConfigValidator {
    private static final Duration MIN_SESSION_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration MIN_TOKEN_TTL = Duration.ofDays(1);

    @Override
    public void validate(Object configObject) throws ConfigValidationException {
        if (!(configObject instanceof UserConfiguration config)) {
            return;
        }
        if (config.enableGlobalSessionTimeout() && config.globalSessionTimeoutInterval().compareTo(MIN_SESSION_TIMEOUT) < 0) {
            throw new ConfigValidationException("Global session timeout interval must be at least 1 second.");
        }
        final LocalDateTime reference = LocalDateTime.of(2000, 1, 1, 0, 0);
        if (reference.plus(config.defaultTTLForNewTokens()).isBefore(reference.plus(MIN_TOKEN_TTL))) {
            throw new ConfigValidationException("Default TTL for new tokens must be at least 1 day.");
        }
    }
}
