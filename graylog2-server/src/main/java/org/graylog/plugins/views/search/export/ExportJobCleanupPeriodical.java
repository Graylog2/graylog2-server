package org.graylog.plugins.views.search.export;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class ExportJobCleanupPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ExportJobCleanupPeriodical.class);
    private final ExportJobService exportJobService;

    @VisibleForTesting
    static final long DEFAULT_MAX_EXPORT_JOB_AGE = TimeUnit.HOURS.toMillis(1L);

    @Inject
    public ExportJobCleanupPeriodical(ExportJobService exportJobService) {
        this.exportJobService = exportJobService;
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
        return Ints.saturatedCast(TimeUnit.HOURS.toSeconds(1L));
    }

    @Override
    protected Logger getLogger() {
        return null;
    }

    @Override
    public void doRun() {
        final DateTime notOlderThan = DateTime.now(DateTimeZone.UTC).minus(DEFAULT_MAX_EXPORT_JOB_AGE);
        LOG.debug("Removing export jobs created before " + notOlderThan);
        exportJobService.removeExpired(notOlderThan);
    }
}
