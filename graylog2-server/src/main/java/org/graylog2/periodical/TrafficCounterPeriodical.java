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
package org.graylog2.periodical;

import jakarta.inject.Inject;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.system.traffic.TrafficCounterCalculator;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class TrafficCounterPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(TrafficCounterPeriodical.class);
    private final Set<TrafficCounterCalculator> trafficCounterCalculators;

    @Inject
    public TrafficCounterPeriodical(Set<TrafficCounterCalculator> trafficCounterCalculators) {
        this.trafficCounterCalculators = trafficCounterCalculators;
    }

    @Override
    public void doRun() {
        final DateTime now = Tools.nowUTC();
        final int secondOfMinute = now.getSecondOfMinute();
        // on the top of every minute, we calculate the traffic
        if (secondOfMinute == 0) {
            trafficCounterCalculators.forEach(trafficCounterCalculator -> {
                DateTime previousMinute = now.minusMinutes(1);
                trafficCounterCalculator.calculate(previousMinute);
            });
        }
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
