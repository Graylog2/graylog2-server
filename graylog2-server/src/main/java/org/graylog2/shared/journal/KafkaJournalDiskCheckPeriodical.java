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
package org.graylog2.shared.journal;

import com.github.joschi.jadconfig.util.Size;
import com.google.common.primitives.Ints;
import org.graylog2.cluster.Node;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;

public class KafkaJournalDiskCheckPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaJournalDiskCheckPeriodical.class);

    private final ServerStatus serverStatus;
    private final Node node;
    private final NotificationService notificationService;
    private final int freeSpacePercentLimit;
    private final File journalDir;
    private final boolean checkEnabled;
    private final boolean journalEnabled;
    private final int periodSeconds;

    @Inject
    public KafkaJournalDiskCheckPeriodical(BaseConfiguration configuration,
                                           KafkaJournalConfiguration kafkaJournalConfiguration,
                                           ServerStatus serverStatus,
                                           Node node,
                                           NotificationService notificationService) {
        this.serverStatus = serverStatus;
        this.node = node;
        this.notificationService = notificationService;

        this.checkEnabled = kafkaJournalConfiguration.isMessageJournalCheckEnabled();
        this.journalEnabled = configuration.isMessageJournalEnabled();
        this.journalDir = kafkaJournalConfiguration.getMessageJournalDir();
        this.freeSpacePercentLimit = kafkaJournalConfiguration.getMessageJournalCheckDiskFreePercent();
        this.periodSeconds = Ints.saturatedCast(kafkaJournalConfiguration.getMessageJournalCheckInterval().getMillis() / 1000L);
    }

    @Override
    public void doRun() {
        final long totalSpace = journalDir.getTotalSpace();
        final long freeSpace = journalDir.getFreeSpace();
        final long freeSpacePercent = (long) (100L * ((double) freeSpace / (double) totalSpace));

        if (freeSpacePercent < freeSpacePercentLimit) {
            LOG.warn("Only {} ({}%) left on disk hosting \"{}\". Setting server status to DEAD.",
                    Size.bytes(freeSpace), freeSpacePercent, journalDir.getAbsolutePath());

            final Notification notification = notificationService.buildNow()
                    .addType(Notification.Type.JOURNAL_INSUFFICIENT_DISK_SPACE)
                    .addSeverity(Notification.Severity.URGENT)
                    .addNode(node)
                    .addDetail("journal_dir", journalDir.getAbsolutePath())
                    .addDetail("disk_total_bytes", totalSpace)
                    .addDetail("disk_free_bytes", freeSpace)
                    .addDetail("disk_free_percent", freeSpacePercent);
            notificationService.publishIfFirst(notification);

            serverStatus.overrideLoadBalancerDead();
        }
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
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return journalEnabled && checkEnabled;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 60;
    }

    @Override
    public int getPeriodSeconds() {
        return periodSeconds;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
