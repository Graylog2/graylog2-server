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

import org.graylog2.plugin.validate.ClusterConfigValidator;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Period;

public class SidecarConfigurationValidator implements ClusterConfigValidator {
    private static final Duration MIN_INACTIVE_THRESHOLD = Duration.standardSeconds(1);
    private static final Duration MIN_EXPIRATION_THRESHOLD = Duration.standardMinutes(1);
    private static final Duration MIN_UPDATE_INTERVAL = Duration.standardSeconds(1);

    @Override
    public void validate(Object configObject) throws ConfigValidationException {
        if (!(configObject instanceof SidecarConfiguration config)) {
            return;
        }
        final Duration inactiveThreshold = toDuration(config.sidecarInactiveThreshold());
        final Duration expirationThreshold = toDuration(config.sidecarExpirationThreshold());
        final Duration updateInterval = toDuration(config.sidecarUpdateInterval());

        if (inactiveThreshold.isShorterThan(MIN_INACTIVE_THRESHOLD)) {
            throw new ConfigValidationException("Inactive threshold must be at least 1 second.");
        }
        if (expirationThreshold.isShorterThan(MIN_EXPIRATION_THRESHOLD)) {
            throw new ConfigValidationException("Expiration threshold must be at least 1 minute.");
        }
        if (updateInterval.isShorterThan(MIN_UPDATE_INTERVAL)) {
            throw new ConfigValidationException("Update interval must be at least 1 second.");
        }
        if (!updateInterval.isShorterThan(inactiveThreshold) || !updateInterval.isShorterThan(expirationThreshold)) {
            throw new ConfigValidationException("Update interval must be less than the inactive and expiration thresholds.");
        }
    }

    private static Duration toDuration(Period period) {
        return period.toDurationFrom(new Instant(0));
    }
}
