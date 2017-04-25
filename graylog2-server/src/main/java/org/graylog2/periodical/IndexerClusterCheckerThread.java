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

import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.cluster.NodeFileDescriptorStats;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

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

        if (!cluster.health().isPresent()) {
            LOG.info("Indexer not fully initialized yet. Skipping periodic cluster check.");
            return;
        }

        boolean allHigher = true;
        final Set<NodeFileDescriptorStats> fileDescriptorStats = cluster.getFileDescriptorStats();
        for (NodeFileDescriptorStats nodeFileDescriptorStats : fileDescriptorStats) {
            final String name = nodeFileDescriptorStats.name();
            final String host = nodeFileDescriptorStats.host();
            final long maxFileDescriptors = nodeFileDescriptorStats.fileDescriptorMax().orElse(-1L);

            if (maxFileDescriptors != -1L && maxFileDescriptors < MINIMUM_OPEN_FILES_LIMIT) {
                // Write notification.
                final Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.ES_OPEN_FILES)
                        .addSeverity(Notification.Severity.URGENT)
                        .addDetail("hostname", host)
                        .addDetail("max_file_descriptors", maxFileDescriptors);

                if (notificationService.publishIfFirst(notification)) {
                    LOG.warn("Indexer node <{}> open file limit is too low: [{}]. Set it to at least {}.",
                            name,
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
