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
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.telemetry.client.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TelemetrySubmissionPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetrySubmissionPeriodical.class);
    private final TelemetryClient telemetryClient;
    private final Map<String, TelemetryMetricSupplier> metricsProviders;
    private static final Duration runPeriod = Duration.days(1);
    private final boolean isEnabled;

    @Inject
    public TelemetrySubmissionPeriodical(TelemetryClient telemetryClient,
                                         TelemetryConfiguration telemetryConfiguration,
                                         Map<String, TelemetryMetricSupplier> metricsProviders) {
        this.telemetryClient = telemetryClient;
        this.metricsProviders = metricsProviders;
        this.isEnabled = telemetryConfiguration.isTelemetryEnabled();
    }

    @Override
    public void doRun() {
        final var telemetryMetrics = metricsProviders.entrySet()
                .stream()
                .map(entry -> entry(entry.getKey(), entry.getValue().get()))
                .flatMap(entry -> entry.getValue().map(metrics -> entry(entry.getKey(), metrics)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        try {
            if (!telemetryMetrics.isEmpty()) {
                telemetryClient.capture(telemetryMetrics);
            }
        } catch (Exception e) {
            LOG.warn("Error while submitting telemetry: ", e);
        }
    }

    private <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
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
        return isEnabled;
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
