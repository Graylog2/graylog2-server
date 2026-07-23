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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorRetentionPeriodicalTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    @Mock
    private CollectorInstanceService collectorInstanceService;
    @Mock
    private CollectorsConfigService collectorsConfigService;
    @Mock
    private FleetTransactionLogService txnLogService;

    private CollectorRetentionPeriodical periodical;

    @BeforeEach
    void setUp() {
        periodical = new CollectorRetentionPeriodical(collectorInstanceService, collectorsConfigService,
                txnLogService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void doRunUsesConfiguredThreshold() {
        final var config = CollectorsConfig.createDefaultBuilder("host")
                .collectorExpirationThreshold(Duration.ofDays(3))
                .build();

        when(collectorsConfigService.getOrDefault()).thenReturn(config);
        when(collectorInstanceService.deleteExpired(Duration.ofDays(3))).thenReturn(2L);

        periodical.doRun();

        verify(collectorInstanceService).deleteExpired(Duration.ofDays(3));
    }

    @Test
    void doRunUsesDefaultThresholdWhenNoConfig() {
        when(collectorsConfigService.getOrDefault()).thenReturn(CollectorsConfig.createDefault("localhost"));
        when(collectorInstanceService.deleteExpired(CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD)).thenReturn(0L);

        periodical.doRun();

        verify(collectorInstanceService).deleteExpired(CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD);
    }

    @Test
    void doRunPurgesTransactionLogWithMarginOverExpiration() {
        final var config = CollectorsConfig.createDefaultBuilder("host")
                .collectorExpirationThreshold(Duration.ofDays(3))
                .build();

        when(collectorsConfigService.getOrDefault()).thenReturn(config);

        periodical.doRun();

        verify(txnLogService).purgeMarkers(NOW.minus(Duration.ofDays(3 + 30)), 100);
    }

    @Test
    void doRunPurgesExpiredInstancesBeforeTransactionLog() {
        when(collectorsConfigService.getOrDefault()).thenReturn(CollectorsConfig.createDefault("localhost"));

        periodical.doRun();

        final var order = inOrder(collectorInstanceService, txnLogService);
        order.verify(collectorInstanceService).deleteExpired(any());
        order.verify(txnLogService).purgeMarkers(any(), anyInt());
    }

    @Test
    void periodicalConfiguration() {
        assertThat(periodical.runsForever()).isFalse();
        assertThat(periodical.stopOnGracefulShutdown()).isTrue();
        assertThat(periodical.leaderOnly()).isTrue();
        assertThat(periodical.startOnThisNode()).isTrue();
        assertThat(periodical.isDaemon()).isTrue();
        assertThat(periodical.getPeriodSeconds()).isEqualTo(300);
    }
}
