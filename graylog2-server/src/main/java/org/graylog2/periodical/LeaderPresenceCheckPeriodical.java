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

import com.google.common.base.Stopwatch;
import org.graylog2.Configuration;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.leader.LeaderElectionMode;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Duration;

/**
 * Periodically checks if there is a leader in the cluster and, if not, emits a notification
 */
@Singleton
public class LeaderPresenceCheckPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderPresenceCheckPeriodical.class);

    private final Configuration configuration;
    private final NodeService nodeService;
    private final NotificationService notificationService;
    private final ActivityWriter activityWriter;
    private final ServerStatus serverStatus;

    private final Stopwatch timeWithoutLeader;

    @Inject
    public LeaderPresenceCheckPeriodical(Configuration configuration,
                                         NodeService nodeService,
                                         NotificationService notificationService,
                                         ActivityWriter activityWriter,
                                         ServerStatus serverStatus) {
        this.configuration = configuration;
        this.nodeService = nodeService;
        this.notificationService = notificationService;
        this.activityWriter = activityWriter;
        this.serverStatus = serverStatus;

        timeWithoutLeader = Stopwatch.createUnstarted();
    }

    @Override
    public void doRun() {
        try {
            final boolean anyLeaderPresent = nodeService.isAnyLeaderPresent();
            if (anyLeaderPresent) {
                if (timeWithoutLeader.isRunning()) {
                    timeWithoutLeader.reset();
                }
                if (fixNoLeaderNotification()) {
                    activityWriter.write(
                            new Activity("Notification condition [" + NotificationImpl.Type.NO_LEADER + "] " +
                                    "has been fixed.", LeaderPresenceCheckPeriodical.class));
                }
            } else {
                if (!timeWithoutLeader.isRunning()) {
                    timeWithoutLeader.start();
                }

                getLogger().debug("No leader node found. Elapsed time without leader node: {}", timeWithoutLeader);

                if (shouldWarnForNoLeader()) {
                    Notification notification = notificationService.buildNow()
                            .addNode(serverStatus.getNodeId().toString())
                            .addType(Notification.Type.NO_LEADER)
                            .addSeverity(Notification.Severity.URGENT);
                    notificationService.publishIfFirst(notification);
                }
            }
        } catch (Exception e) {
            LOG.warn("Caught exception during check for presence of leader node.", e);
        }
    }

    private boolean shouldWarnForNoLeader() {
        if (configuration.getLeaderElectionMode() != LeaderElectionMode.AUTOMATIC) {
            return true;
        }
        // In automatic leader election mode, there is an expected time window between a node giving up the leader lock
        // and another node claiming the lock, where there is no leader in the cluster. This time window should not
        // exceed the configured polling interval of the leader lock in a healthy cluster. So if a cluster is still
        // without a leader after consecutive checks, we'll consider this an erroneous condition. This doesn't cover
        // cases of rapid thrashing of the leader status, but it's hopefully good enough.
        final Duration gracePeriod = configuration.getLeaderElectionLockPollingInterval().plusSeconds(1);
        return timeWithoutLeader.elapsed().compareTo(gracePeriod) > 0;
    }

    private boolean fixNoLeaderNotification() {
        // intentional non-short-circuit boolean operator to also remove legacy notification
        //noinspection deprecation
        return notificationService.fixed(notificationService.build().addType(Notification.Type.NO_MASTER)) |
                notificationService.fixed(notificationService.build().addType(Notification.Type.NO_LEADER));
    }

    @Nonnull
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
        return false;
    }

    @Override
    public boolean leaderOnly() {
        return false;
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
        return 5;
    }
}
