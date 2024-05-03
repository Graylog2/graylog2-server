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
package org.graylog2.telemetry.scheduler;

import com.github.joschi.jadconfig.util.Duration;
import jakarta.inject.Inject;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.telemetry.client.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

public class TelemetrySubmissionPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetrySubmissionPeriodical.class);
    private final TelemetryClient telemetryClient;
    private final Map<String, TelemetryMetricSupplier> metricsProviders;
    private static final Duration runPeriod = Duration.days(1);

    @Inject
    public TelemetrySubmissionPeriodical(TelemetryClient telemetryClient,
                                         Map<String, TelemetryMetricSupplier> metricsProviders) {
        this.telemetryClient = telemetryClient;
        this.metricsProviders = metricsProviders;
    }

    @Override
    public void doRun() {
        if (telemetryClient.isEnabled()) {
            final var telemetryMetrics = metricsProviders.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
            telemetryMetrics.forEach((key, value) -> value.map(TelemetryEvent::metrics)
                    .ifPresent(metrics -> telemetryClient.capture(key, metrics)));
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
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return Math.toIntExact(runPeriod.toSeconds());
    }

    @Override
    public int getPeriodSeconds() {
        return Math.toIntExact(runPeriod.toSeconds());
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
