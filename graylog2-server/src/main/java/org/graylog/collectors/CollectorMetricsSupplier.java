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
package org.graylog.collectors;

import jakarta.inject.Inject;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

public class CollectorMetricsSupplier implements TelemetryMetricSupplier {

    private final Clock clock;
    private final CollectorsConfigService collectorsConfigService;
    private final FleetTransactionLogService fleetTransactionLogService;
    private final CollectorInstanceService collectorInstanceService;
    private final FleetService fleetService;
    private final SourceService sourceService;
    private final EnrollmentTokenService enrollmentTokenService;

    @Inject
    public CollectorMetricsSupplier(Clock clock,
                                    CollectorsConfigService collectorsConfigService,
                                    FleetTransactionLogService fleetTransactionLogService,
                                    CollectorInstanceService collectorInstanceService,
                                    FleetService fleetService,
                                    SourceService sourceService,
                                    EnrollmentTokenService enrollmentTokenService) {
        this.clock = clock;
        this.collectorsConfigService = collectorsConfigService;
        this.fleetTransactionLogService = fleetTransactionLogService;
        this.collectorInstanceService = collectorInstanceService;
        this.fleetService = fleetService;
        this.sourceService = sourceService;
        this.enrollmentTokenService = enrollmentTokenService;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        final CollectorsConfig collectorsConfig = collectorsConfigService.getOrDefault();
        final var enrollmentTokenStats = enrollmentTokenService.getStats();

        return Optional.of(TelemetryEvent.of(Map.ofEntries(
                entry("transactions_last_day", fleetTransactionLogService.countMarkersSince(Instant.now(clock).minus(1, ChronoUnit.DAYS))),
                entry("total_collectors", collectorInstanceService.count()),
                entry("online_collectors", collectorInstanceService.countOnline(Instant.now().minus(collectorsConfig.collectorOfflineThreshold()))),
                entry("fleets", fleetService.count()),
                entry("sources", sourceService.count()),
                entry("source_types", sourceService.countByType()),
                entry("heartbeat_interval_seconds", collectorsConfig.collectorHeartbeatInterval().toSeconds()),
                entry("visibility_threshold_seconds", collectorsConfig.collectorDefaultVisibilityThreshold().toSeconds()),
                entry("offline_threshold_seconds", collectorsConfig.collectorOfflineThreshold().toSeconds()),
                entry("expiration_threshold_seconds", collectorsConfig.collectorExpirationThreshold().toSeconds()),
                entry("collector_cert_lifetime_seconds", collectorsConfig.collectorCertLifetime().toSeconds()),
                entry("enrollment_tokens", enrollmentTokenStats.count()),
                entry("enrollment_token_total_usage", enrollmentTokenStats.totalUsage()),
                entry("enrollment_tokens_expired", enrollmentTokenStats.expired())
        )));
    }
}
