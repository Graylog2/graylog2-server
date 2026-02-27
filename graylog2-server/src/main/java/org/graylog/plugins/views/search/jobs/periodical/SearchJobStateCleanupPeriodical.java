/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.jobs.periodical;

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.jobs.SearchJobStateService;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchJobStateCleanupPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(SearchJobStateCleanupPeriodical.class);
    static final Duration MIN_AGE_TO_REMOVE = Duration.standardDays(7);
    static final Duration MIN_AGE_TO_EXPIRE = Duration.standardDays(1);

    private final SearchJobStateService searchJobStateService;

    @Inject
    public SearchJobStateCleanupPeriodical(final SearchJobStateService searchJobStateService) {
        this.searchJobStateService = searchJobStateService;
    }

    @Override
    public void doRun() {
        removeOldJobs();
        expireOldEnoughJobs();
    }

    private void removeOldJobs() {
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC).minus(MIN_AGE_TO_REMOVE);
        final long numRemoved = searchJobStateService.deleteOlderThan(dateTime);
        LOG.debug("Removed search job state documents : " + numRemoved);
    }

    private void expireOldEnoughJobs() {
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC).minus(MIN_AGE_TO_EXPIRE);
        final long numExpired = searchJobStateService.expireOlderThan(dateTime);
        LOG.debug("Expired search job state documents : " + numExpired);
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
        return 60;
    }

    @Override
    public int getPeriodSeconds() {
        return 5 * 60;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
