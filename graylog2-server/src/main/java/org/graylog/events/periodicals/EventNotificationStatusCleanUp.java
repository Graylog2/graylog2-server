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
package org.graylog.events.periodicals;

import org.graylog.events.notifications.DBNotificationGracePeriodService;
import org.graylog.events.notifications.EventNotificationStatus;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class EventNotificationStatusCleanUp extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(EventNotificationStatusCleanUp.class);

    private final DBNotificationGracePeriodService dbNotificationGracePeriodService;
    private JobSchedulerClock clock;

    // Remove status entries after a day
    private static final long OUTOFDATE_IN_MS = 86400000;

    @Inject
    public EventNotificationStatusCleanUp(DBNotificationGracePeriodService dbNotificationGracePeriodService,
                                          JobSchedulerClock clock) {
        this.dbNotificationGracePeriodService = dbNotificationGracePeriodService;
        this.clock = clock;
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
    public boolean primaryOnly() {
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
        return 120;
    }

    @Override
    public int getPeriodSeconds() {
        return 86400;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        int deleted = 0;
        List<EventNotificationStatus> eventNotificationStatuses = dbNotificationGracePeriodService.getAllStatuses();
        for (EventNotificationStatus status : eventNotificationStatuses) {
            if (status.triggeredAt().isPresent()) {
                DateTime triggeredAt = status.triggeredAt().get();
                if (triggeredAt.isBefore(clock.nowUTC().minusMillis((int) OUTOFDATE_IN_MS)) && status.gracePeriodMs() < OUTOFDATE_IN_MS) {
                    deleted = deleted + dbNotificationGracePeriodService.deleteStatus(status.id());
                }
            }

            if (deleted > 0) {
                LOG.debug("Deleted {} outdated notification statuses.", deleted);
            }
        }
    }
}
