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
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;

/**
 * Periodically cleans up old read notifications based on the configured retention period,
 * and enforces a safety cap on total collection size.
 */
public class SystemNotificationCleanupPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(SystemNotificationCleanupPeriodical.class);
    private static final int SAFETY_CAP = 10_000;

    private final SystemNotificationService systemNotificationService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public SystemNotificationCleanupPeriodical(SystemNotificationService systemNotificationService,
                                               ClusterConfigService clusterConfigService) {
        this.systemNotificationService = systemNotificationService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public void doRun() {
        final SystemNotificationRetentionConfig config = clusterConfigService.getOrDefault(
                SystemNotificationRetentionConfig.class,
                SystemNotificationRetentionConfig.createDefault()
        );

        // Step 1: Delete read entries older than the retention period
        final Instant cutoff = Instant.now().minus(Duration.ofDays(config.retentionDays()));
        final long deletedByAge = systemNotificationService.deleteOlderThan(cutoff);
        if (deletedByAge > 0) {
            LOG.info("Deleted {} read notifications older than {} days", deletedByAge, config.retentionDays());
        }

        // Step 2: Safety cap -- delete oldest entries if collection still exceeds limit
        final long deletedByExcess = systemNotificationService.deleteExcess(SAFETY_CAP);
        if (deletedByExcess > 0) {
            LOG.info("Deleted {} notifications to enforce safety cap of {}", deletedByExcess, SAFETY_CAP);
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
    public boolean leaderOnly() {
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
        return 120;
    }

    @Override
    public int getPeriodSeconds() {
        return 3600;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
