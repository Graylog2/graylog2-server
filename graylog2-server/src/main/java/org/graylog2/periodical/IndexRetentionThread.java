/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.periodical;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
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
    private final Cluster cluster;
    private final ClusterConfigService clusterConfigService;
    private final NodeId nodeId;
    private final NotificationService notificationService;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategyMap;

    @Inject
    public IndexRetentionThread(ElasticsearchConfiguration configuration,
                                Cluster cluster,
                                ClusterConfigService clusterConfigService,
                                NodeId nodeId,
                                NotificationService notificationService,
                                Map<String, Provider<RetentionStrategy>> retentionStrategyMap) {
        this.configuration = configuration;
        this.cluster = cluster;
        this.clusterConfigService = clusterConfigService;
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

        final IndexManagementConfig config = clusterConfigService.get(IndexManagementConfig.class);

        if (config == null) {
            LOG.warn("No index management configuration found, not running index retention!");
            retentionProblemNotification("Index Retention Problem!",
                    "No index management configuration found, not running index retention! Please fix your index retention configuration!");
            return;
        }

        final Provider<RetentionStrategy> retentionStrategyProvider = retentionStrategyMap.get(config.retentionStrategy());

        if (retentionStrategyProvider == null) {
            LOG.warn("Retention strategy \"{}\" not found, not running index retention!", config.retentionStrategy());
            retentionProblemNotification("Index Retention Problem!",
                    "Index retention strategy " + config.retentionStrategy() + " not found! Please fix your index retention configuration!");
            return;
        }

        final RetentionStrategy retentionStrategy = retentionStrategyProvider.get();

        retentionStrategy.retain();
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