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
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurgeExpiredCollectorInstancesPeriodicalTest {

    @Mock
    private CollectorInstanceService collectorInstanceService;
    @Mock
    private ClusterConfigService clusterConfigService;

    private PurgeExpiredCollectorInstancesPeriodical periodical;

    @BeforeEach
    void setUp() {
        periodical = new PurgeExpiredCollectorInstancesPeriodical(collectorInstanceService, clusterConfigService);
    }

    @Test
    void doRunUsesConfiguredThreshold() {
        final var config = new CollectorsConfig(null, null, null,
                new IngestEndpointConfig(true, "host", 14401, null),
                new IngestEndpointConfig(false, "host", 14402, null),
                CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD,
                CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD,
                Duration.ofDays(3));
        when(clusterConfigService.getOrDefault(CollectorsConfig.class, CollectorsConfig.DEFAULT)).thenReturn(config);
        when(collectorInstanceService.deleteExpired(Duration.ofDays(3))).thenReturn(2L);

        periodical.doRun();

        verify(collectorInstanceService).deleteExpired(Duration.ofDays(3));
    }

    @Test
    void doRunUsesDefaultThresholdWhenNoConfig() {
        when(clusterConfigService.getOrDefault(CollectorsConfig.class, CollectorsConfig.DEFAULT))
                .thenReturn(CollectorsConfig.DEFAULT);
        when(collectorInstanceService.deleteExpired(CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD)).thenReturn(0L);

        periodical.doRun();

        verify(collectorInstanceService).deleteExpired(CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD);
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
