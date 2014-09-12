/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.periodical;

import com.google.inject.Inject;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.graylog2.indexer.Indexer;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerClusterCheckerThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerClusterCheckerThread.class);
    private static final int MINIMUM_OPEN_FILES_LIMIT = 64000;

    private final Indexer indexer;
    private final NotificationService notificationService;

    @Inject
    public IndexerClusterCheckerThread(final NotificationService notificationService,
                                       final Indexer indexer) {
        this.notificationService = notificationService;
        this.indexer = indexer;
    }

    @Override
    public void doRun() {
        if (!notificationService.isFirst(Notification.Type.ES_OPEN_FILES)) {
            return;
        }

        if (null == indexer.cluster()) {
            LOG.info("Indexer not fully initialized yet. Skipping periodic cluster check.");
            return;
        }

        boolean allHigher = true;
        for (NodeInfo node : indexer.cluster().getDataNodes()) {
            // Check number of maximum open files.
            final String osName = node.getJvm().getSystemProperties().get("os.name");
            if (osName.startsWith("Windows")) {
                LOG.debug("Skipping open file limit check for Indexer node <{}> on Windows", node.getNode().getName());
            } else if (node.getProcess().getMaxFileDescriptors() < MINIMUM_OPEN_FILES_LIMIT) {
                // Write notification.
                final Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.ES_OPEN_FILES)
                        .addSeverity(Notification.Severity.URGENT);

                if (notificationService.publishIfFirst(notification)) {
                    LOG.warn("Indexer node <{}> open file limit is too low: [{}]. Set it to at least {}.",
                            node.getNode().getName(),
                            node.getProcess().getMaxFileDescriptors(),
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
