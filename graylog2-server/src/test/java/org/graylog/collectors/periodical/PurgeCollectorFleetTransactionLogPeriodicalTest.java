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
package org.graylog.collectors.periodical;

import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetTransactionLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurgeCollectorFleetTransactionLogPeriodicalTest {

    @Mock
    private FleetTransactionLogService fleetTransactionLogService;
    @Mock
    private CollectorInstanceService collectorInstanceService;
    @Mock
    private CollectorsConfigService collectorsConfigService;

    private PurgeCollectorFleetTransactionLogPeriodical periodical;

    @BeforeEach
    void setUp() {
        periodical = new PurgeCollectorFleetTransactionLogPeriodical(
                fleetTransactionLogService, collectorInstanceService, collectorsConfigService);
    }

    @Test
    void doRunPurgesOldMarkersWithConfiguredRetention() {
        final var config = CollectorsConfig.createDefaultBuilder("host")
                .collectorTransactionLogRetentionThreshold(Duration.ofDays(14))
                .build();

        when(collectorsConfigService.getOrDefault()).thenReturn(config);
        when(collectorInstanceService.getMinLastProcessedTxnSeq()).thenReturn(50L);
        when(fleetTransactionLogService.deleteOldMarkers(any(Instant.class), eq(50L))).thenReturn(3L);

        periodical.doRun();

        verify(fleetTransactionLogService).deleteOldMarkers(any(Instant.class), eq(50L));
    }

    @Test
    void doRunUsesDefaultRetentionWhenNotConfigured() {
        final var config = CollectorsConfig.createDefault("localhost");

        when(collectorsConfigService.getOrDefault()).thenReturn(config);
        when(collectorInstanceService.getMinLastProcessedTxnSeq()).thenReturn(Long.MAX_VALUE);
        when(fleetTransactionLogService.deleteOldMarkers(any(Instant.class), eq(Long.MAX_VALUE))).thenReturn(0L);

        periodical.doRun();

        verify(fleetTransactionLogService).deleteOldMarkers(any(Instant.class), eq(Long.MAX_VALUE));
    }

    @Test
    void periodicalConfiguration() {
        assertThat(periodical.runsForever()).isFalse();
        assertThat(periodical.stopOnGracefulShutdown()).isTrue();
        assertThat(periodical.leaderOnly()).isTrue();
        assertThat(periodical.startOnThisNode()).isTrue();
        assertThat(periodical.isDaemon()).isTrue();
        assertThat(periodical.getInitialDelaySeconds()).isEqualTo(120);
        assertThat(periodical.getPeriodSeconds()).isEqualTo(86400);
    }
}
