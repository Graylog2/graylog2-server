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

import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.cluster.health.AbsoluteValueWatermarkSettings;
import org.graylog2.indexer.cluster.health.ByteSize;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.PercentageWatermarkSettings;
import org.graylog2.indexer.cluster.health.SIUnitParser;
import org.graylog2.indexer.cluster.health.WatermarkSettings;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IndexerClusterCheckerThreadTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Cluster cluster;

    @Mock
    private NotificationService notificationService;

    private IndexerClusterCheckerThread indexerClusterCheckerThread;

    @Before
    public void setUp() {
        indexerClusterCheckerThread = new IndexerClusterCheckerThread(notificationService, cluster);
    }

    @Test
    public void preventOpenFilesNotificationFlood() {
        when(notificationService.isFirst(Notification.Type.ES_OPEN_FILES)).thenReturn(false);

        indexerClusterCheckerThread.checkOpenFiles();

        verify(notificationService, times(1)).isFirst(Notification.Type.ES_OPEN_FILES);
        verifyNoMoreInteractions(notificationService);
        verifyZeroInteractions(cluster);
    }

    @Test
    public void noChecksWhenDiskAllocationThresholdIsDisabled() throws Exception {
        ClusterAllocationDiskSettings clusterAllocationDiskSettings = mock(ClusterAllocationDiskSettings.class);

        when(clusterAllocationDiskSettings.ThresholdEnabled()).thenReturn(false);
        when(cluster.getClusterAllocationDiskSettings()).thenReturn(clusterAllocationDiskSettings);

        indexerClusterCheckerThread.checkDiskUsage();

        verify(clusterAllocationDiskSettings, times(1)).ThresholdEnabled();
        verifyNoMoreInteractions(clusterAllocationDiskSettings);
        verify(cluster, times(1)).getClusterAllocationDiskSettings();
        verifyNoMoreInteractions(cluster);
    }

    @Test
    public void fixAllDiskUsageNotificationsAbsolute() throws Exception {
        Set<NodeDiskUsageStats> nodeDiskUsageStats = mockNodeDiskUsageStats();
        when(cluster.getDiskUsageStats()).thenReturn(nodeDiskUsageStats);
        when(cluster.getClusterAllocationDiskSettings()).thenReturn(buildThresholdNotTriggeredClusterAllocationDiskSettings(WatermarkSettings.SettingsType.ABSOLUTE));

        indexerClusterCheckerThread.checkDiskUsage();

        verify(notificationService, never()).publishIfFirst(any());
        verify(notificationService, times(1)).fixed(Notification.Type.ES_NODE_DISK_WATERMARK_FLOOD_STAGE);
        verify(notificationService, times(1)).fixed(Notification.Type.ES_NODE_DISK_WATERMARK_HIGH);
        verify(notificationService, times(1)).fixed(Notification.Type.ES_NODE_DISK_WATERMARK_LOW);
    }

    @Test
    public void fixAllDiskUsageNotificationsPercentage() throws Exception {
        Set<NodeDiskUsageStats> nodeDiskUsageStats = mockNodeDiskUsageStats();
        when(cluster.getDiskUsageStats()).thenReturn(nodeDiskUsageStats);
        when(cluster.getClusterAllocationDiskSettings()).thenReturn(buildThresholdNotTriggeredClusterAllocationDiskSettings(WatermarkSettings.SettingsType.PERCENTAGE));

        indexerClusterCheckerThread.checkDiskUsage();

        verify(notificationService, never()).publishIfFirst(any());
        verify(notificationService, times(1)).fixed(Notification.Type.ES_NODE_DISK_WATERMARK_FLOOD_STAGE);
        verify(notificationService, times(1)).fixed(Notification.Type.ES_NODE_DISK_WATERMARK_HIGH);
        verify(notificationService, times(1)).fixed(Notification.Type.ES_NODE_DISK_WATERMARK_LOW);
    }

    @Test
    public void notificationCreatesWhenLowThresholdTriggeredAbsolute() throws Exception {
        notificationCreated(Notification.Type.ES_NODE_DISK_WATERMARK_LOW, WatermarkSettings.SettingsType.ABSOLUTE);
    }

    @Test
    public void notificationCreatesWhenLowThresholdTriggeredPercentage() throws Exception {
        notificationCreated(Notification.Type.ES_NODE_DISK_WATERMARK_LOW, WatermarkSettings.SettingsType.PERCENTAGE);
    }

    @Test
    public void notificationCreatesWhenHighThresholdTriggeredAbsolute() throws Exception {
        notificationCreated(Notification.Type.ES_NODE_DISK_WATERMARK_HIGH, WatermarkSettings.SettingsType.ABSOLUTE);
    }

    @Test
    public void notificationCreatesWhenHighThresholdTriggeredPercentage() throws Exception {
        notificationCreated(Notification.Type.ES_NODE_DISK_WATERMARK_HIGH, WatermarkSettings.SettingsType.PERCENTAGE);
    }

    @Test
    public void notificationCreatesWhenFloodStageThresholdTriggeredAbsolute() throws Exception {
        notificationCreated(Notification.Type.ES_NODE_DISK_WATERMARK_FLOOD_STAGE, WatermarkSettings.SettingsType.ABSOLUTE);
    }

    @Test
    public void notificationCreatesWhenFloodStageThresholdTriggeredPercentage() throws Exception {
        notificationCreated(Notification.Type.ES_NODE_DISK_WATERMARK_FLOOD_STAGE, WatermarkSettings.SettingsType.PERCENTAGE);
    }

    private void notificationCreated(Notification.Type notificationType, WatermarkSettings.SettingsType watermarkSettingsType) throws Exception {
        Set<NodeDiskUsageStats> nodeDiskUsageStats = mockNodeDiskUsageStats();
        when(cluster.getDiskUsageStats()).thenReturn(nodeDiskUsageStats);
        if (watermarkSettingsType == WatermarkSettings.SettingsType.ABSOLUTE) {
            when(cluster.getClusterAllocationDiskSettings()).thenReturn(buildThresholdTriggeredClusterAllocationDiskSettings(notificationType, WatermarkSettings.SettingsType.ABSOLUTE));
        } else {
            when(cluster.getClusterAllocationDiskSettings()).thenReturn(buildThresholdTriggeredClusterAllocationDiskSettings(notificationType, WatermarkSettings.SettingsType.PERCENTAGE));
        }
        when(notificationService.isFirst(notificationType)).thenReturn(true);
        Notification notification = new NotificationImpl();
        when(notificationService.buildNow()).thenReturn(notification);

        indexerClusterCheckerThread.checkDiskUsage();

        ArgumentCaptor<Notification> argument = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(1)).publishIfFirst(argument.capture());

        Notification publishedNotification = argument.getValue();
        assertThat(publishedNotification.getType()).isEqualTo(notificationType);
    }

    private Set<NodeDiskUsageStats> mockNodeDiskUsageStats() {
        Set<NodeDiskUsageStats> nodesDiskUsageStats = new HashSet<>();
        NodeDiskUsageStats nodeDiskUsageStats = mock(NodeDiskUsageStats.class);

        when(nodeDiskUsageStats.ip()).thenReturn("0.0.0.0");
        when(nodeDiskUsageStats.diskTotal()).thenReturn(SIUnitParser.parseBytesSizeValue("100GB"));
        when(nodeDiskUsageStats.diskUsed()).thenReturn(SIUnitParser.parseBytesSizeValue("70GB"));
        when(nodeDiskUsageStats.diskAvailable()).thenReturn(SIUnitParser.parseBytesSizeValue("30GB"));
        when(nodeDiskUsageStats.diskUsedPercent()).thenReturn(70D);

        nodesDiskUsageStats.add(nodeDiskUsageStats);
        return nodesDiskUsageStats;
    }

    private ClusterAllocationDiskSettings buildThresholdNotTriggeredClusterAllocationDiskSettings(WatermarkSettings.SettingsType type) {
        if (type == WatermarkSettings.SettingsType.ABSOLUTE) {
            AbsoluteValueWatermarkSettings absoluteValueWatermarkSettings = new AbsoluteValueWatermarkSettings.Builder()
                    .low(SIUnitParser.parseBytesSizeValue("15GB"))
                    .high(SIUnitParser.parseBytesSizeValue("10GB"))
                    .floodStage(SIUnitParser.parseBytesSizeValue("5GB"))
                    .build();
            return ClusterAllocationDiskSettings.create(true, absoluteValueWatermarkSettings);
        } else {
            PercentageWatermarkSettings percentageWatermarkSettings = new PercentageWatermarkSettings.Builder()
                    .low(85D)
                    .high(90D)
                    .floodStage(95D)
                    .build();
            return ClusterAllocationDiskSettings.create(true, percentageWatermarkSettings);
        }
    }

    private ClusterAllocationDiskSettings buildThresholdTriggeredClusterAllocationDiskSettings(Notification.Type expectedNotificationType, WatermarkSettings.SettingsType type) {
        if (type == WatermarkSettings.SettingsType.ABSOLUTE) {
            return buildThresholdTriggeredClusterAllocationDiskSettingsAbsolute(expectedNotificationType);
        } else {
            return buildThresholdTriggeredClusterAllocationDiskSettingsPercentage(expectedNotificationType);
        }
    }

    private ClusterAllocationDiskSettings buildThresholdTriggeredClusterAllocationDiskSettingsAbsolute(Notification.Type expectedNotificationType) {
        ByteSize low;
        ByteSize high;
        ByteSize floodStage;
        if (expectedNotificationType == Notification.Type.ES_NODE_DISK_WATERMARK_LOW) {
            low = SIUnitParser.parseBytesSizeValue("35GB");
            high = SIUnitParser.parseBytesSizeValue("10GB");
            floodStage = SIUnitParser.parseBytesSizeValue("5GB");
        } else if (expectedNotificationType == Notification.Type.ES_NODE_DISK_WATERMARK_HIGH) {
            low = SIUnitParser.parseBytesSizeValue("45GB");
            high = SIUnitParser.parseBytesSizeValue("35GB");
            floodStage = SIUnitParser.parseBytesSizeValue("5GB");
        } else {
            low = SIUnitParser.parseBytesSizeValue("55GB");
            high = SIUnitParser.parseBytesSizeValue("45GB");
            floodStage = SIUnitParser.parseBytesSizeValue("35GB");
        }
        return ClusterAllocationDiskSettings.create(true, new AbsoluteValueWatermarkSettings.Builder()
                .low(low)
                .high(high)
                .floodStage(floodStage)
                .build());
    }

    public ClusterAllocationDiskSettings buildThresholdTriggeredClusterAllocationDiskSettingsPercentage(Notification.Type expectedNotificationType) {
        double low;
        double high;
        double floodStage;
        if (expectedNotificationType == Notification.Type.ES_NODE_DISK_WATERMARK_LOW) {
            low = 25D;
            high = 90D;
            floodStage = 95D;
        } else if (expectedNotificationType == Notification.Type.ES_NODE_DISK_WATERMARK_HIGH) {
            low = 15D;
            high = 25D;
            floodStage = 95D;
        } else {
            low = 5D;
            high = 15D;
            floodStage = 25D;
        }
        return ClusterAllocationDiskSettings.create(true, new PercentageWatermarkSettings.Builder()
                .low(low)
                .high(high)
                .floodStage(floodStage)
                .build());
    }
}
