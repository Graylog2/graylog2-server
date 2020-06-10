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
package org.graylog.scheduler.periodicals;

import org.graylog.scheduler.DBJobTriggerService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class ScheduleTriggerCleanUp extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduleTriggerCleanUp.class);

    private final DBJobTriggerService dbJobTriggerService;

    // Remove completed job triggers after a day
    private static final long OUTOFDATE_IN_DAYS = 1;

    @Inject
    public ScheduleTriggerCleanUp(DBJobTriggerService dbJobTriggerService) {
        this.dbJobTriggerService = dbJobTriggerService;
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
    public boolean parentOnly() {
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
        int deleted = dbJobTriggerService.deleteCompletedOnceSchedulesOlderThan(1, TimeUnit.DAYS);
        if (deleted > 0) {
            LOG.debug("Deleted {} outdated OnceJobSchedule triggers.", deleted);
        }
    }
}
