package org.graylog.scheduler.periodicals;

import org.graylog.scheduler.DBJobTriggerService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ScheduleTriggerCleanUp extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduleTriggerCleanUp.class);

    private final DBJobTriggerService dbJobTriggerService;

    // Remove completed job triggers after a day
    private static final long OUTOFDATE_IN_MS = 86400000;

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
        int deleted = dbJobTriggerService.deleteCompletedOnceSchedulesOlderThan(OUTOFDATE_IN_MS);
        if (deleted > 0) {
            LOG.debug("Deleted {} outdated OnceJobSchedule triggers.", deleted);
        }
    }
}
