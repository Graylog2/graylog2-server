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
package org.graylog2.notifications;

import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemNotificationCleanupPeriodicalTest {

    @Mock
    private SystemNotificationService systemNotificationService;

    @Mock
    private ClusterConfigService clusterConfigService;

    private SystemNotificationCleanupPeriodical periodical;

    @BeforeEach
    void setUp() {
        periodical = new SystemNotificationCleanupPeriodical(systemNotificationService, clusterConfigService);
    }

    @Test
    void isLeaderOnly() {
        assertThat(periodical.leaderOnly()).isTrue();
    }

    @Test
    void runsHourly() {
        assertThat(periodical.getPeriodSeconds()).isEqualTo(3600);
    }

    @Test
    void doRunUsesConfiguredRetentionDays() {
        when(clusterConfigService.getOrDefault(eq(SystemNotificationRetentionConfig.class), any()))
                .thenReturn(SystemNotificationRetentionConfig.create(7));
        when(systemNotificationService.deleteOlderThan(any())).thenReturn(0L);
        when(systemNotificationService.deleteExcess(anyInt())).thenReturn(0L);

        periodical.doRun();

        final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(systemNotificationService).deleteOlderThan(cutoffCaptor.capture());

        // The cutoff should be approximately 7 days ago
        final Instant sevenDaysAgo = Instant.now().minusSeconds(7 * 24 * 3600);
        assertThat(cutoffCaptor.getValue()).isBetween(
                sevenDaysAgo.minusSeconds(60),
                sevenDaysAgo.plusSeconds(60));
    }

    @Test
    void doRunEnforcesSafetyCap() {
        when(clusterConfigService.getOrDefault(eq(SystemNotificationRetentionConfig.class), any()))
                .thenReturn(SystemNotificationRetentionConfig.createDefault());
        when(systemNotificationService.deleteOlderThan(any())).thenReturn(0L);
        when(systemNotificationService.deleteExcess(anyInt())).thenReturn(0L);

        periodical.doRun();

        verify(systemNotificationService).deleteExcess(10_000);
    }

    @Test
    void doRunUsesDefaultConfigWhenNoneConfigured() {
        when(clusterConfigService.getOrDefault(eq(SystemNotificationRetentionConfig.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(systemNotificationService.deleteOlderThan(any())).thenReturn(0L);
        when(systemNotificationService.deleteExcess(anyInt())).thenReturn(0L);

        periodical.doRun();

        final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(systemNotificationService).deleteOlderThan(cutoffCaptor.capture());

        // Default is 30 days
        final Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 3600);
        assertThat(cutoffCaptor.getValue()).isBetween(
                thirtyDaysAgo.minusSeconds(60),
                thirtyDaysAgo.plusSeconds(60));
    }
}
