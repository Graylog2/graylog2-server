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

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;

public class IndexRetentionThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRetentionThread.class);

    private final ElasticsearchConfiguration configuration;
    private final IndexSetRegistry indexSetRegistry;
    private final Cluster cluster;
    private final NodeId nodeId;
    private final NotificationService notificationService;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategyMap;

    @Inject
    public IndexRetentionThread(ElasticsearchConfiguration configuration,
                                IndexSetRegistry indexSetRegistry,
                                Cluster cluster,
                                NodeId nodeId,
                                NotificationService notificationService,
                                Map<String, Provider<RetentionStrategy>> retentionStrategyMap) {
        this.configuration = configuration;
        this.indexSetRegistry = indexSetRegistry;
        this.cluster = cluster;
        this.nodeId = nodeId;
        this.notificationService = notificationService;
        this.retentionStrategyMap = retentionStrategyMap;
    }

    @Override
    public void doRun() {
        if (!cluster.isConnected() || !cluster.isHealthy()) {
            LOG.info("Elasticsearch cluster not available, skipping index retention checks.");
            return;
        }

        for (final IndexSet indexSet : indexSetRegistry) {
            if (!indexSet.getConfig().isWritable()) {
                LOG.debug("Skipping non-writable index set <{}> ({})", indexSet.getConfig().id(), indexSet.getConfig().title());
                continue;
            }
            final IndexSetConfig config = indexSet.getConfig();
            final Provider<RetentionStrategy> retentionStrategyProvider = retentionStrategyMap.get(config.retentionStrategyClass());

            if (retentionStrategyProvider == null) {
                LOG.warn("Retention strategy \"{}\" not found, not running index retention!", config.retentionStrategyClass());
                retentionProblemNotification("Index Retention Problem!",
                        "Index retention strategy " + config.retentionStrategyClass() + " not found! Please fix your index retention configuration!");
                continue;
            }

            retentionStrategyProvider.get().retain(indexSet);
        }
    }

    private void retentionProblemNotification(String title, String description) {
        final Notification notification = notificationService.buildNow()
                .addNode(nodeId.toString())
                .addType(Notification.Type.GENERIC)
                .addSeverity(Notification.Severity.URGENT)
                .addDetail("title", title)
                .addDetail("description", description);
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
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return configuration.performRetention();
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return (int) MINUTES.toSeconds(5);
    }
}