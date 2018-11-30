package org.graylog.plugins.sidecar.periodical;

import org.graylog.plugins.sidecar.services.ImportService;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PurgeExpiredConfigurationUploads extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(PurgeExpiredConfigurationUploads.class);

    private final ImportService importService;

    @Inject
    public PurgeExpiredConfigurationUploads(ImportService importService) {
        this.importService = importService;
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
        return 60 * 10;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Period outOfUseThreshold = new Period().withDays(30);
        final int purgedUploads = importService.destroyExpired(outOfUseThreshold);
        LOG.debug("Purged {} outdated configuration uploads.", purgedUploads);
    }
}
