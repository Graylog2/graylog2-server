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
package org.graylog2.shared.buffers.processors;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

public record TimeStampConfig(@JsonProperty("grace_period") Duration gracePeriod) {
    public static final TimeStampConfig THRESHOLD_2DAYS = new TimeStampConfig(Duration.ofDays(2));
    private static final TimeStampConfig THRESHOLD_DISTANT_FUTURE = new TimeStampConfig(Duration.ofSeconds(1000000000000L));

    public static TimeStampConfig getDefault() {
        // Off by default
        return THRESHOLD_DISTANT_FUTURE;
    }
}
