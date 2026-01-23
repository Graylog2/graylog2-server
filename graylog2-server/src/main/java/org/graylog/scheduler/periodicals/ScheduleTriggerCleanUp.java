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
package org.graylog.scheduler.periodicals;

import jakarta.inject.Inject;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.DBSystemJobTriggerService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ScheduleTriggerCleanUp extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduleTriggerCleanUp.class);

    private final DBJobTriggerService dbJobTriggerService;
    private final DBSystemJobTriggerService dbSystemJobTriggerService;

    // Remove completed job triggers after a day
    private static final long OUTOFDATE_IN_DAYS = 1;

    @Inject
    public ScheduleTriggerCleanUp(DBJobTriggerService dbJobTriggerService,
                                  DBSystemJobTriggerService dbSystemJobTriggerService) {
        this.dbJobTriggerService = dbJobTriggerService;
        this.dbSystemJobTriggerService = dbSystemJobTriggerService;
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
    public boolean leaderOnly() {
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
        int deleted = dbJobTriggerService.deleteCompletedOnceSchedulesOlderThan(OUTOFDATE_IN_DAYS, TimeUnit.DAYS);
        if (deleted > 0) {
            LOG.debug("Deleted {} outdated OnceJobSchedule triggers.", deleted);
        }

        int deletedSystem = dbSystemJobTriggerService.deleteCompletedOnceSchedulesOlderThan(OUTOFDATE_IN_DAYS, TimeUnit.DAYS);
        if (deletedSystem > 0) {
            LOG.debug("Deleted {} completed system job triggers.", deletedSystem);
        }
    }
}
