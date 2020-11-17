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

import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.inputs.InputRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ClusterHealthCheckThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterHealthCheckThread.class);
    private NotificationService notificationService;
    private final InputRegistry inputRegistry;
    private final NodeId nodeId;
    private boolean isCloud;

    @Inject
    public ClusterHealthCheckThread(NotificationService notificationService,
                                    InputRegistry inputRegistry,
                                    NodeId nodeId,
                                    @Named("is_cloud") boolean isCloud) {
        this.notificationService = notificationService;
        this.inputRegistry = inputRegistry;
        this.nodeId = nodeId;
        this.isCloud = isCloud;
    }

    @Override
    public void doRun() {
        if (isCloud) {
            LOG.debug("Skipping run of ClusterHealthCheckThread, since contained checks are not applicable for Cloud.");
            return;
        }
        try {
            if (inputRegistry.runningCount() == 0) {
                LOG.debug("No input running in cluster!");
                notificationService.publishIfFirst(getNotification());
            } else {
                LOG.debug("Running inputs found, disabling notification");
                notificationService.fixed(getNotification());
            }
        } catch (NodeNotFoundException e) {
            LOG.error("Unable to find own node: ", e.getMessage(), e);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected Notification getNotification() throws NodeNotFoundException {
        Notification notification = notificationService.buildNow();
        notification.addType(Notification.Type.NO_INPUT_RUNNING);
        notification.addSeverity(Notification.Severity.URGENT);
        notification.addNode(nodeId.toString());

        return notification;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean isDaemon() {
        return true;
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
    public int getInitialDelaySeconds() {
        // Wait some time until all inputs have been started otherwise this will trigger a notification on every
        // startup of the server.
        return 120;
    }

    @Override
    public int getPeriodSeconds() {
        return 20;
    }

}
