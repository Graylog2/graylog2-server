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

import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link Periodical} to check if index ranges need to be recalculated and notify the administrators about it.
 *
 * @see <a href="https://github.com/Graylog2/graylog2-server/pull/1274">Refactor index ranges handling (#1274)</a>
 */
public class IndexRangesMigrationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRangesMigrationPeriodical.class);

    private final Cluster cluster;
    private final Indices indices;
    private final IndexRangeService indexRangeService;
    private final NotificationService notificationService;

    @Inject
    public IndexRangesMigrationPeriodical(final Cluster cluster,
                                          final Indices indices,
                                          final IndexRangeService indexRangeService,
                                          final NotificationService notificationService) {
        this.cluster = checkNotNull(cluster);
        this.indices = checkNotNull(indices);
        this.indexRangeService = checkNotNull(indexRangeService);
        this.notificationService = checkNotNull(notificationService);
    }

    @Override
    public void doRun() {
        while (!cluster.isConnected() || !cluster.isHealthy()) {
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
        }

        final int numberOfIndices = indices.getAll().size();
        final int numberOfIndexRanges = indexRangeService.findAll().size();
        if (numberOfIndices > numberOfIndexRanges) {
            LOG.info("There are more indices ({}) than there are index ranges ({}). Notifying administrator.",
                    numberOfIndices, numberOfIndexRanges);
            final Notification notification = notificationService.buildNow()
                    .addSeverity(Notification.Severity.URGENT)
                    .addType(Notification.Type.INDEX_RANGES_RECALCULATION)
                    .addDetail("indices", numberOfIndices)
                    .addDetail("index_ranges", numberOfIndexRanges);
            notificationService.publishIfFirst(notification);
        }
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
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
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
