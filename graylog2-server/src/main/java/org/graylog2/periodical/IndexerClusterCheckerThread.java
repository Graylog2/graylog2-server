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

import com.google.common.annotations.VisibleForTesting;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.cluster.health.AbsoluteValueWatermarkSettings;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.health.PercentageWatermarkSettings;
import org.graylog2.indexer.cluster.health.WatermarkSettings;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;

public class IndexerClusterCheckerThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerClusterCheckerThread.class);
    private static final int MINIMUM_OPEN_FILES_LIMIT = 64000;

    private final NotificationService notificationService;
    private final Cluster cluster;

    @Inject
    public IndexerClusterCheckerThread(NotificationService notificationService,
                                       Cluster cluster) {
        this.notificationService = notificationService;
        this.cluster = cluster;
    }

    @Override
    public void doRun() {
        if (!cluster.health().isPresent()) {
            LOG.info("Indexer not fully initialized yet. Skipping periodic cluster check.");
            return;
        }
        checkOpenFiles();
        checkDiskUsage();
    }

    @VisibleForTesting
    void checkOpenFiles() {
        if (notificationExists(Notification.Type.ES_OPEN_FILES)) {
            return;
        }

        boolean allHigher = true;
        final Set<NodeFileDescriptorStats> fileDescriptorStats = cluster.getFileDescriptorStats();
        for (NodeFileDescriptorStats nodeFileDescriptorStats : fileDescriptorStats) {
            final String name = nodeFileDescriptorStats.name();
            final String ip = nodeFileDescriptorStats.ip();
            final String host = nodeFileDescriptorStats.host();
            final long maxFileDescriptors = nodeFileDescriptorStats.fileDescriptorMax().orElse(-1L);

            if (maxFileDescriptors != -1L && maxFileDescriptors < MINIMUM_OPEN_FILES_LIMIT) {
                // Write notification.
                final String ipOrHostName = firstNonNull(host, ip);
                final Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.ES_OPEN_FILES)
                        .addSeverity(Notification.Severity.URGENT)
                        .addDetail("hostname", ipOrHostName)
                        .addDetail("max_file_descriptors", maxFileDescriptors);

                if (notificationService.publishIfFirst(notification)) {
                    LOG.warn("Indexer node <{}> ({}) open file limit is too low: [{}]. Set it to at least {}.",
                            name,
                            ipOrHostName,
                            maxFileDescriptors,
                            MINIMUM_OPEN_FILES_LIMIT);
                }
                allHigher = false;
            }
        }

        if (allHigher) {
            Notification notification = notificationService.build().addType(Notification.Type.ES_OPEN_FILES);
            notificationService.fixed(notification);
        }
    }

    @VisibleForTesting()
    void checkDiskUsage() {
        final Map<Notification.Type, List<String>> notificationTypePerNodeIdentifier = new HashMap<>();
        try {
            ClusterAllocationDiskSettings settings = cluster.getClusterAllocationDiskSettings();
            if (settings.ThresholdEnabled()) {
                final Set<NodeDiskUsageStats> diskUsageStats = cluster.getDiskUsageStats();

                for (NodeDiskUsageStats nodeDiskUsageStats : diskUsageStats) {
                    Notification.Type currentNodeNotificationType = null;
                    WatermarkSettings<?> watermarkSettings = settings.watermarkSettings();
                    if (watermarkSettings instanceof PercentageWatermarkSettings) {
                        currentNodeNotificationType = getDiskUsageNotificationTypeByPercentage((PercentageWatermarkSettings) watermarkSettings, nodeDiskUsageStats);
                    } else if (watermarkSettings instanceof AbsoluteValueWatermarkSettings) {
                        currentNodeNotificationType = getDiskUsageNotificationTypeByAbsoluteValues((AbsoluteValueWatermarkSettings) watermarkSettings, nodeDiskUsageStats);
                    }
                    if (currentNodeNotificationType != null) {
                        String nodeIdentifier = firstNonNull(nodeDiskUsageStats.host(), nodeDiskUsageStats.ip());
                        if (notificationTypePerNodeIdentifier.containsKey(currentNodeNotificationType)) {
                            List<String> nodesIdentifier = notificationTypePerNodeIdentifier.get(currentNodeNotificationType);
                            nodesIdentifier.add(nodeIdentifier);
                        } else {
                            notificationTypePerNodeIdentifier.put(currentNodeNotificationType, Arrays.asList(nodeIdentifier));
                        }
                    }
                }

                if (notificationTypePerNodeIdentifier.isEmpty()) {
                    fixAllDiskUsageNotifications();
                } else {
                    publishDiskUsageNotifications(notificationTypePerNodeIdentifier);
                }
            }
        } catch (Exception e) {
            LOG.error("Error while trying to check Elasticsearch disk usage.Details: " + e.getMessage());
        }
    }

    private Notification.Type getDiskUsageNotificationTypeByPercentage(PercentageWatermarkSettings settings, NodeDiskUsageStats nodeDiskUsageStats) {
        if (settings.floodStage() != null && nodeDiskUsageStats.diskUsedPercent() >= settings.floodStage()) {
            return Notification.Type.ES_NODE_DISK_WATERMARK_FLOOD_STAGE;
        } else if (nodeDiskUsageStats.diskUsedPercent() >= settings.high()) {
            return Notification.Type.ES_NODE_DISK_WATERMARK_HIGH;
        } else if (nodeDiskUsageStats.diskUsedPercent() >= settings.low()) {
            return Notification.Type.ES_NODE_DISK_WATERMARK_LOW;
        }
        return null;
    }

    private Notification.Type getDiskUsageNotificationTypeByAbsoluteValues(AbsoluteValueWatermarkSettings settings, NodeDiskUsageStats nodeDiskUsageStats) {
        if (settings.floodStage() != null && nodeDiskUsageStats.diskAvailable().getBytes() <= settings.floodStage().getBytes()) {
            return Notification.Type.ES_NODE_DISK_WATERMARK_FLOOD_STAGE;
        } else if (nodeDiskUsageStats.diskAvailable().getBytes() <= settings.high().getBytes()) {
            return Notification.Type.ES_NODE_DISK_WATERMARK_HIGH;
        } else if (nodeDiskUsageStats.diskAvailable().getBytes() <= settings.low().getBytes()) {
            return Notification.Type.ES_NODE_DISK_WATERMARK_LOW;
        }
        return null;
    }

    private void fixAllDiskUsageNotifications() {
        notificationService.fixed(Notification.Type.ES_NODE_DISK_WATERMARK_FLOOD_STAGE);
        notificationService.fixed(Notification.Type.ES_NODE_DISK_WATERMARK_HIGH);
        notificationService.fixed(Notification.Type.ES_NODE_DISK_WATERMARK_LOW);
    }

    private void publishDiskUsageNotifications(Map<Notification.Type, List<String>> notificationTypePerNodeIdentifier) {
        for (Map.Entry<Notification.Type, List<String>> entry : notificationTypePerNodeIdentifier.entrySet()) {
            if (!notificationExists(entry.getKey())) {
                Notification notification = notificationService.buildNow()
                        .addType(entry.getKey())
                        .addSeverity(Notification.Severity.URGENT)
                        .addDetail("nodes", String.join(", ", entry.getValue()));
                notificationService.publishIfFirst(notification);
                for (String node: entry.getValue()) {
                    LOG.warn("Elasticsearch node [{}] triggered [{}] due to low free disk space",
                            node,
                            entry.getKey());
                }
            }
        }
    }

    private boolean notificationExists(Notification.Type type) {
        return !notificationService.isFirst(type);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
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
    public boolean masterOnly() {
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
        return 30;
    }
}
