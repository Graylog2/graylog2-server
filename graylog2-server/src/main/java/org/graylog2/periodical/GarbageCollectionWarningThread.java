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
import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import org.graylog2.Configuration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public class GarbageCollectionWarningThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(GarbageCollectionWarningThread.class);

    private final List<GarbageCollectorMXBean> garbageCollectors;
    private final Duration gcWarningThreshold;
    private final NodeId nodeId;
    private final NotificationService notificationService;

    @Inject
    public GarbageCollectionWarningThread(final Configuration configuration,
                                          final NodeId nodeId,
                                          final NotificationService notificationService) {
        this(configuration.getGcWarningThreshold(), nodeId, notificationService);
    }

    GarbageCollectionWarningThread(final Duration gcWarningThreshold,
                                   final NodeId nodeId,
                                   final NotificationService notificationService) {
        this(ManagementFactory.getGarbageCollectorMXBeans(), gcWarningThreshold, nodeId, notificationService);
    }

    GarbageCollectionWarningThread(final List<GarbageCollectorMXBean> garbageCollectors,
                                   final Duration gcWarningThreshold,
                                   final NodeId nodeId,
                                   final NotificationService notificationService) {
        this.garbageCollectors = garbageCollectors;
        this.gcWarningThreshold = gcWarningThreshold;
        this.nodeId = nodeId;
        this.notificationService = notificationService;
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
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        for (final GarbageCollectorMXBean gc : garbageCollectors) {
            final NotificationEmitter emitter = (NotificationEmitter) gc;
            final NotificationListener listener = new NotificationListener() {
                @Override
                public void handleNotification(javax.management.Notification notification, Object handback) {
                    if (GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
                        final GcInfo gcInfo = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData()).getGcInfo();
                        final Duration duration = Duration.milliseconds(gcInfo.getDuration());

                        if (duration.compareTo(gcWarningThreshold) > 0) {
                            LOG.warn("Last GC run with {} took longer than {} (last duration={})",
                                    gc.getName(), gcWarningThreshold, duration);

                            final Notification systemNotification = notificationService.buildNow()
                                    .addNode(nodeId.toString())
                                    .addTimestamp(Tools.iso8601())
                                    .addSeverity(Notification.Severity.URGENT)
                                    .addType(Notification.Type.GC_TOO_LONG)
                                    .addDetail("gc_name", gc.getName())
                                    .addDetail("gc_duration_ms", duration.toMilliseconds())
                                    .addDetail("gc_threshold_ms", gcWarningThreshold.toMilliseconds())
                                    .addDetail("gc_collection_count", gc.getCollectionCount())
                                    .addDetail("gc_collection_time", gc.getCollectionTime());

                            if (!notificationService.publishIfFirst(systemNotification)) {
                                LOG.debug("Couldn't publish notification: {}", notification);
                            }
                        }
                    }
                }
            };
            emitter.addNotificationListener(listener, null, null);
        }
    }
}
