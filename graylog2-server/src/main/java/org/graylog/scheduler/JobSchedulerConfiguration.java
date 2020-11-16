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
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import org.graylog2.plugin.PluginConfigBean;

/**
 * Job scheduler specific configuration fields for the server configuration file.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess"})
public class JobSchedulerConfiguration implements PluginConfigBean {
    public static final String LOOP_SLEEP_DURATION = "job_scheduler_loop_sleep_duration";

    @Parameter(value = LOOP_SLEEP_DURATION, validators = PositiveDurationValidator.class)
    private Duration loopSleepDuration = Duration.seconds(1);

    public Duration getLoopSleepDuration() {
        return loopSleepDuration;
    }
}
