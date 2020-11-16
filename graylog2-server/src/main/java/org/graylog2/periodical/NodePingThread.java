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

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class NodePingThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(NodePingThread.class);
    private final NodeService nodeService;
    private final NotificationService notificationService;
    private final ActivityWriter activityWriter;
    private final HttpConfiguration httpConfiguration;
    private final ServerStatus serverStatus;

    @Inject
    public NodePingThread(NodeService nodeService,
                          NotificationService notificationService,
                          ActivityWriter activityWriter,
                          HttpConfiguration httpConfiguration,
                          ServerStatus serverStatus) {
        this.nodeService = nodeService;
        this.notificationService = notificationService;
        this.activityWriter = activityWriter;
        this.httpConfiguration = httpConfiguration;
        this.serverStatus = serverStatus;
    }

    @Override
    public void doRun() {
        final boolean isMaster = serverStatus.hasCapability(ServerStatus.Capability.MASTER);
        try {
            Node node = nodeService.byNodeId(serverStatus.getNodeId());
            nodeService.markAsAlive(node, isMaster, httpConfiguration.getHttpPublishUri().resolve(HttpConfiguration.PATH_API));
        } catch (NodeNotFoundException e) {
            LOG.warn("Did not find meta info of this node. Re-registering.");
            nodeService.registerServer(serverStatus.getNodeId().toString(),
                    isMaster,
                    httpConfiguration.getHttpPublishUri().resolve(HttpConfiguration.PATH_API),
                    Tools.getLocalCanonicalHostname());
        }
        try {
            // Remove old nodes that are no longer running. (Just some housekeeping)
            nodeService.dropOutdated();

            // Check that we still have a master node in the cluster, if not, warn the user.
            if (nodeService.isAnyMasterPresent()) {
                Notification notification = notificationService.build()
                        .addType(Notification.Type.NO_MASTER);
                boolean removedNotification = notificationService.fixed(notification);
                if (removedNotification) {
                    activityWriter.write(
                        new Activity("Notification condition [" + NotificationImpl.Type.NO_MASTER + "] " +
                                             "has been fixed.", NodePingThread.class));
                }
            } else {
                Notification notification = notificationService.buildNow()
                        .addNode(serverStatus.getNodeId().toString())
                        .addType(Notification.Type.NO_MASTER)
                        .addSeverity(Notification.Severity.URGENT);
                notificationService.publishIfFirst(notification);
            }

        } catch (Exception e) {
            LOG.warn("Caught exception during node ping.", e);
        }
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
        return false;
    }

    @Override
    public boolean masterOnly() {
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
        return 1;
    }
}
