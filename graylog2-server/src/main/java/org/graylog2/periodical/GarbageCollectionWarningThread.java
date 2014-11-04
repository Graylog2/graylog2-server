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

import com.github.joschi.jadconfig.util.Duration;
import org.graylog2.Configuration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public class GarbageCollectionWarningThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(GarbageCollectionWarningThread.class);

    private final List<GarbageCollectorMXBean> garbageCollectors;
    private final Duration gcCheckInterval;
    private final long gcWarningThresholdMillis;
    private final NodeId nodeId;
    private final NotificationService notificationService;

    @Inject
    public GarbageCollectionWarningThread(final Configuration configuration,
                                          final NodeId nodeId,
                                          final NotificationService notificationService) {
        this(configuration.getGcCheckInterval(), configuration.getGcWarningThreshold(), nodeId, notificationService);
    }

    GarbageCollectionWarningThread(final Duration gcCheckInterval,
                                   final Duration gcWarningThreshold,
                                   final NodeId nodeId,
                                   final NotificationService notificationService) {
        this(ManagementFactory.getGarbageCollectorMXBeans(), gcCheckInterval, gcWarningThreshold.toMilliseconds(), nodeId, notificationService);
    }

    GarbageCollectionWarningThread(final List<GarbageCollectorMXBean> garbageCollectors,
                                   final Duration gcCheckInterval,
                                   final long gcWarningThresholdMillis,
                                   final NodeId nodeId,
                                   final NotificationService notificationService) {
        this.garbageCollectors = garbageCollectors;
        this.gcCheckInterval = gcCheckInterval;
        this.gcWarningThresholdMillis = gcWarningThresholdMillis;
        this.nodeId = nodeId;
        this.notificationService = notificationService;
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
        return 10;
    }

    @Override
    public int getPeriodSeconds() {
        return (int) gcCheckInterval.toSeconds();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        for (final GarbageCollectorMXBean gc : garbageCollectors) {
            if (gc.getCollectionTime() >= gcWarningThresholdMillis) {
                LOG.warn("Last GC run with {} took longer than {}ms (count={}, time={}ms)",
                        gc.getName(), gcWarningThresholdMillis, gc.getCollectionCount(), gc.getCollectionTime());

                final Notification notification = notificationService.buildNow()
                        .addNode(nodeId.toString())
                        .addTimestamp(DateTime.now(DateTimeZone.UTC))
                        .addSeverity(Notification.Severity.URGENT)
                        .addType(Notification.Type.GC_TOO_LONG)
                        .addDetail("gc_name", gc.getName())
                        .addDetail("gc_count", gc.getCollectionCount())
                        .addDetail("gc_time", gc.getCollectionTime());

                if (!notificationService.publishIfFirst(notification)) {
                    LOG.debug("Couldn't publish notification: {}", notification);
                }
            }
        }
    }
}
