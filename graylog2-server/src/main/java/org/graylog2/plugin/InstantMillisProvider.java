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
package org.graylog2.plugin;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstantMillisProvider implements DateTimeUtils.MillisProvider {
    private static final Logger log = LoggerFactory.getLogger(InstantMillisProvider.class);
    private DateTime currentTick;

    public InstantMillisProvider(DateTime instant) {
        setTimeTo(instant);
    }

    public void setTimeTo(DateTime instant) {
        log.debug("Setting clock to {}", instant);
        currentTick = instant;
    }

    @Override
    public long getMillis() {
        return currentTick.getMillis();
    }

    public void tick(Period period) {
        currentTick = currentTick.plus(period);
        log.debug("Ticking clock by {} to {}", period, currentTick);
    }
}
