package org.graylog.scheduler;

import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class JobHeartbeatPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(JobHeartbeatPeriodical.class);

    private final JobSchedulerService jobSchedulerService;

    @Inject
    public JobHeartbeatPeriodical(JobSchedulerService jobSchedulerService) {
        this.jobSchedulerService = jobSchedulerService;
    }

    @Override
    public void doRun() {
        jobSchedulerService.updateLockedJobs();
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
        return 55;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
