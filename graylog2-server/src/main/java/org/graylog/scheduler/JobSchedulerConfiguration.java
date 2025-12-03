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
package org.graylog.scheduler;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import org.graylog2.configuration.converters.MapConverter;
import org.graylog2.plugin.PluginConfigBean;

import java.util.Map;

/**
 * Job scheduler specific configuration fields for the server configuration file.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess", "FieldMayBeFinal"})
public class JobSchedulerConfiguration implements PluginConfigBean {
    public static final String LOOP_SLEEP_DURATION = "job_scheduler_loop_sleep_duration";
    public static final String LOCK_EXPIRATION_DURATION = "job_scheduler_lock_expiration_duration";
    public static final String CONCURRENCY_LIMITS = "job_scheduler_concurrency_limits";

    @Parameter(value = LOOP_SLEEP_DURATION, validators = PositiveDurationValidator.class)
    private Duration loopSleepDuration = Duration.seconds(1);

    @Parameter(value = LOCK_EXPIRATION_DURATION, validators = Minimum1MinuteValidator.class)
    private Duration lockExpirationDuration = Duration.minutes(5);

    @Parameter(value = CONCURRENCY_LIMITS, converter = MapConverter.StringInteger.class)
    private Map<String, Integer> concurrencyLimits = Map.of();

    /**
     * Concurrency limits per job type. A missing entry signifies unlimited concurrency. (up to the number of worker threads)
     *
     * @return mapping of job type to max number of worker threads
     */
    public Map<String, Integer> getConcurrencyLimits() {
        return concurrencyLimits;
    }

    public Duration getLoopSleepDuration() {
        return loopSleepDuration;
    }

    public Duration getLockExpirationDuration() {
        return lockExpirationDuration;
    }

    public static class Minimum1MinuteValidator implements Validator<Duration> {
        @Override
        public void validate(final String name, final Duration value) throws ValidationException {
            if (value != null && value.compareTo(Duration.minutes(1)) < 0) {
                throw new ValidationException("Parameter " + name + " should be at least 1 minute (found " + value + ")");
            }
        }
    }
}
