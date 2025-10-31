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
package org.graylog2.plugin.utilities.ratelimitedlog;

import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class RateLimitedLogFactory {

    private static final int DEFAULT_MAX_RATE = 5;
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);

    protected static final int MAX_QUIET_RATE = 1;
    protected static final Duration MAX_QUIET_DURATION = Duration.ofSeconds(60);

    public static RateLimitedLog createRateLimitedLog(final Logger logger,
                                                      final int maxRate,
                                                      final Duration duration) {
        return RateLimitedLog
                .withRateLimit(logger)
                .maxRate(maxRate)
                .every(duration)
                .build();
    }

    public static RateLimitedLog createRateLimitedLog(final Class<?> clazz,
                                                      final int maxRate,
                                                      final Duration duration) {
        return createRateLimitedLog(LoggerFactory.getLogger(clazz), maxRate, duration);
    }

    public static RateLimitedLog createDefaultRateLimitedLog(final Class<?> clazz) {
        return createRateLimitedLog(LoggerFactory.getLogger(clazz), DEFAULT_MAX_RATE, DEFAULT_DURATION);
    }

    /**
     * @return Rate limited log that only prints once per 60 seconds. Use for frequently occurring code paths
     * that you don't want to generate a lot of log noise.
     */
    public static RateLimitedLog createQuietDefaultRateLimitedLog(final Class<?> clazz) {
        return createRateLimitedLog(LoggerFactory.getLogger(clazz), MAX_QUIET_RATE, MAX_QUIET_DURATION);
    }
}
