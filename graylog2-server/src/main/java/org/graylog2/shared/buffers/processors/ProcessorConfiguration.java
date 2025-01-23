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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import org.graylog2.configuration.Documentation;

public class ProcessorConfiguration {
    public java.time.Duration getTimestampGracePeriod() {
        return java.time.Duration.ofMillis(timestampGracePeriod.toMilliseconds());
    }

    @Documentation(visible = false)
    @Parameter(value = "timestamp_grace_period", validators = PositiveDurationValidator.class)
    final private Duration timestampGracePeriod = Duration.days(30);
}
