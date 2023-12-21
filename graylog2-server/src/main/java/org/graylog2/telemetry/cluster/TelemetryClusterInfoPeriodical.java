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
package org.graylog2.telemetry.cluster;

import com.google.common.primitives.Ints;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.telemetry.rest.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.Duration;

import static org.graylog2.configuration.TelemetryConfiguration.TELEMETRY_CLUSTER_INFO_TTL;

public class TelemetryClusterInfoPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryClusterInfoPeriodical.class);
    private final Duration telemetryClusterInfoTtl;
    private final TelemetryService telemetryService;

    @Inject
    public TelemetryClusterInfoPeriodical(@Named(TELEMETRY_CLUSTER_INFO_TTL) Duration telemetryClusterInfoTtl,
                                          TelemetryService telemetryClusterService) {
        this.telemetryClusterInfoTtl = telemetryClusterInfoTtl;
        this.telemetryService = telemetryClusterService;
    }

    @Override
    public void doRun() {
        telemetryService.updateTelemetryClusterData();
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
        return Ints.saturatedCast(telemetryClusterInfoTtl.minusMinutes(1).toSeconds());
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
