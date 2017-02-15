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

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticsearch.monitor.process.ProcessStats;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

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
        if (!notificationService.isFirst(Notification.Type.ES_OPEN_FILES)) {
            return;
        }

        try {
            cluster.health().getStatus();
        } catch (Exception e) {
            LOG.info("Indexer not fully initialized yet. Skipping periodic cluster check.");
            return;
        }

        boolean allHigher = true;
        final Map<String, NodeInfo> nodesInfos = cluster.getDataNodes();
        final Map<String, NodeStats> nodesStats = cluster.getNodesStats(nodesInfos.keySet().toArray(new String[nodesInfos.size()]));


        for (Map.Entry<String, NodeStats> entry : nodesStats.entrySet()) {
            final String nodeId = entry.getKey();
            final NodeStats nodeStats = entry.getValue();
            final NodeInfo nodeInfo = nodesInfos.get(nodeId);
            final String nodeName = nodeInfo.getNode().getName();

            // Check number of maximum open files.
            final ProcessStats processStats = nodeStats.getProcess();
            if (processStats == null) {
                LOG.debug("Couldn't read process stats of Elasticsearch node {}", nodeName);
                return;
            }

            final long maxFileDescriptors = processStats.getMaxFileDescriptors();

            final JvmInfo jvmInfo = nodeInfo.getJvm();
            if (jvmInfo == null) {
                LOG.debug("Couldn't read JVM info of Elasticsearch node {}", nodeName);
                return;
            }

            final String osName = jvmInfo.getSystemProperties().getOrDefault("os.name", "");
            if (osName.startsWith("Windows")) {
                LOG.debug("Skipping open file limit check for Indexer node <{}> on Windows", nodeName);
            } else if (maxFileDescriptors != -1 && maxFileDescriptors < MINIMUM_OPEN_FILES_LIMIT) {
                // Write notification.
                final Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.ES_OPEN_FILES)
                        .addSeverity(Notification.Severity.URGENT)
                        .addDetail("hostname", nodeInfo.getHostname())
                        .addDetail("max_file_descriptors", maxFileDescriptors);

                if (notificationService.publishIfFirst(notification)) {
                    LOG.warn("Indexer node <{}> open file limit is too low: [{}]. Set it to at least {}.",
                            nodeName,
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
