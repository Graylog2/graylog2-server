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

import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class IndexBlockCheck extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRotationThread.class);

    private final NotificationService notificationService;
    private final IndexSetRegistry indexSetRegistry;
    private final Cluster cluster;
    private final Indices indices;
    private final NodeId nodeId;

    @Inject
    public IndexBlockCheck(final NotificationService notificationService,
                           final IndexSetRegistry indexSetRegistry,
                           final Cluster cluster,
                           final Indices indices,
                           final NodeId nodeId) {
        this.notificationService = notificationService;
        this.indexSetRegistry = indexSetRegistry;
        this.cluster = cluster;
        this.indices = indices;
        this.nodeId = nodeId;
    }

    @Override
    public void doRun() {
        if (cluster.isConnected()) {
            final IndicesBlockStatus indicesBlockStatus = indices.getIndicesBlocksStatus(getAllActiveWriteIndices());
            if (indicesBlockStatus.countBlockedIndices() > 0) {
                indicesBlockedProblemNotification("Indices blocked", indicesBlockStatus.countBlockedIndices() + " indices are blocked.", indicesBlockStatus);
            } else {
                notificationService.fixed(Notification.Type.ES_INDEX_BLOCKED);
            }
        } else {
            LOG.debug("Elasticsearch cluster isn't healthy. Skipping index block check.");
        }
    }

    private List<String> getAllActiveWriteIndices() {
        List<String> activeWriteIndices = new ArrayList<>();
        indexSetRegistry.forEach((indexSet) -> {
            try {
                final String activeWriteIndex = indexSet.getActiveWriteIndex();
                if (activeWriteIndex != null) {
                    activeWriteIndices.add(activeWriteIndex);
                }
            } catch (Exception e) {
                LOG.error("Couldn't perform index block check for index set : " + indexSet, e);
            }
        });
        return activeWriteIndices;
    }

    private void indicesBlockedProblemNotification(final String title, final String description, final IndicesBlockStatus indicesBlockStatus) {
        final Notification notification = notificationService.buildNow()
                .addNode(nodeId.toString())
                .addType(Notification.Type.ES_INDEX_BLOCKED)
                .addSeverity(Notification.Severity.URGENT)
                .addDetail("title", title)
                .addDetail("description", description)
                .addDetail("blockDetails", indicesBlockStatus.toBlockDetails());
        notificationService.publishIfFirst(notification);
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
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 30;
    }
}
