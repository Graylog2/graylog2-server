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
package org.graylog2.shared.initializers;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.types.ObjectId;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.FixedIDs;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobSchedule;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.periodical.LegacyPeriodicalSchedulerJob;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class PeriodicalsService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicalsService.class);

    private final Periodicals periodicals;
    private final ServerStatus serverStatus;
    private final Set<Periodical> periodicalSet;
    private final DBJobDefinitionService jobDefinitionService;
    private final DBJobTriggerService jobTriggerService;
    private final JobSchedulerClock clock;

    @Inject
    public PeriodicalsService(Periodicals periodicals,
                              ServerStatus serverStatus,
                              Set<Periodical> periodicalSet, DBJobDefinitionService jobDefinitionService, DBJobTriggerService jobTriggerService, JobSchedulerClock clock) {
        this.periodicals = periodicals;
        this.serverStatus = serverStatus;
        this.periodicalSet = periodicalSet;
        this.jobDefinitionService = jobDefinitionService;
        this.jobTriggerService = jobTriggerService;
        this.clock = clock;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting {} periodicals ...", periodicalSet.size());

        final Set<Periodical> masterOnlyPeriodicals = new HashSet<>();
        final Set<Periodical> otherPeriodicals = new HashSet<>();

        for (Periodical periodical : periodicalSet) {
            if (periodical.masterOnly()) {
                masterOnlyPeriodicals.add(periodical);
            } else {
                otherPeriodicals.add(periodical);
            }
        }

        scheduleSingletonPeriodicals(masterOnlyPeriodicals);
        otherPeriodicals.forEach(this::startPeriodical);
    }

    private void scheduleSingletonPeriodicals(Set<Periodical> periodicals) {
        // TODO: no need to save this in mongo. Use some kind of in-memory system job definitions
        final JobDefinitionDto jobDefinition = jobDefinitionService.save(JobDefinitionDto.builder()
                .id(FixedIDs.LEGACY_PERIODICALS)
                .title("legacy periodicals")
                .description("runs legacy periodicals")
                .config(new LegacyPeriodicalSchedulerJob.JobDefinitionConfig())
                .build());

        periodicals.forEach(periodical -> {
            // TODO: We need some safe fixed IDs for the triggers. This is incredibly unsafe!!
            // First three bytes of the hash of the class name, interpreted as
            final int counter = new BigInteger(1, Arrays.copyOf(DigestUtils.sha256(periodical.getClass().getCanonicalName()), 3)).intValue();
            final ObjectId triggerId =
                    new ObjectId(new Date(1000), 0, (short) 0, counter);
            final LegacyPeriodicalSchedulerJob.Data triggerData = LegacyPeriodicalSchedulerJob.Data.builder()
                    .periodicalClass(periodical.getClass().getCanonicalName())
                    .build();

            JobSchedule schedule = LegacyPeriodicalSchedulerJob.buildSchedule(periodical);

            jobTriggerService.saveSingleton(JobTriggerDto.builder()
                    .id(triggerId.toString())
                    .jobDefinitionId(jobDefinition.id())
                    .data(triggerData)
                    .schedule(schedule)
                    .nextTime(clock.nowUTC().plusSeconds(periodical.getInitialDelaySeconds()))
                    .build());
        });
    }

    private void startPeriodical(Periodical periodical) {
        try {
            periodical.initialize();

            if (!periodical.startOnThisNode()) {
                LOG.info("Not starting [{}] periodical. Not configured to run on this node.", periodical.getClass().getCanonicalName());
                return;
            }

            // Register and start.
            periodicals.registerAndStart(periodical);
        } catch (Exception e) {
            LOG.error("Could not initialize periodical.", e);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        for (Periodical periodical : periodicals.getAllStoppedOnGracefulShutdown()) {
            LOG.info("Shutting down periodical [{}].", periodical.getClass().getCanonicalName());
            Stopwatch s = Stopwatch.createStarted();

            // Cancel future executions.
            Map<Periodical, ScheduledFuture> futures = periodicals.getFutures();
            if (futures.containsKey(periodical)) {
                futures.get(periodical).cancel(false);

                s.stop();
                LOG.info("Shutdown of periodical [{}] complete, took <{}ms>.",
                        periodical.getClass().getCanonicalName(), s.elapsed(TimeUnit.MILLISECONDS));
            } else {
                LOG.error("Could not find periodical [{}] in futures list. Not stopping execution.",
                        periodical.getClass().getCanonicalName());
            }
        }
    }
}
