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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.cluster.leader.LeaderChangedEvent;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
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
    private final EventBus eventBus;
    private final LeaderElectionService leaderElectionService;

    private final Set<Periodical> leaderOnlyPeriodicals = new HashSet<>();
    private final Set<Periodical> allNodePeriodicals = new HashSet<>();

    @Inject
    public PeriodicalsService(Periodicals periodicals,
                              ServerStatus serverStatus,
                              Set<Periodical> periodicalSet, EventBus eventBus, LeaderElectionService leaderElectionService) {
        this.periodicals = periodicals;
        this.serverStatus = serverStatus;
        this.periodicalSet = periodicalSet;
        this.eventBus = eventBus;
        this.leaderElectionService = leaderElectionService;

        periodicalSet.forEach(p -> {
            if (p.masterOnly()) {
                leaderOnlyPeriodicals.add(p);
            } else {
                allNodePeriodicals.add(p);
            }
        });
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);

        if (leaderElectionService.isLeader()) {
            LOG.info("Starting {} periodicals ...", periodicalSet.size());
            startPeriodicals(periodicalSet);
        } else {
            LOG.info("Starting {} periodicals ...", allNodePeriodicals.size());
            LOG.info("Delaying start of {} periodicals until this node becomes leader ...", leaderOnlyPeriodicals.size());
            startPeriodicals(allNodePeriodicals);
        }
    }

    @Subscribe
    public void leaderChanged(LeaderChangedEvent leaderChangedEvent) {
        if (!leaderElectionService.isLeader()) {
            return;
        }
        LOG.info("Starting {} periodicals ...", leaderOnlyPeriodicals.size());
        startPeriodicals(leaderOnlyPeriodicals);
    }

    private synchronized void startPeriodicals(Set<Periodical> periodicalsToStart) {
        final Sets.SetView<Periodical> notYetRunningPeriodicals =
                Sets.difference(periodicalsToStart, ImmutableSet.copyOf(periodicals.getAll()));

        int numOfPeriodicalsToSkip = periodicalsToStart.size() - notYetRunningPeriodicals.size();

        if (numOfPeriodicalsToSkip > 0) {
            LOG.warn("Skipping start of {} periodicals which have already been started.", numOfPeriodicalsToSkip);
        }

        for (Periodical periodical : notYetRunningPeriodicals) {
            try {
                periodical.initialize();

                if (periodical.masterOnly() && !leaderElectionService.isLeader()) {
                    LOG.error("Not starting [{}] periodical. Periodical requires node to be leader.", periodical.getClass().getCanonicalName());
                    continue;
                }

                if (!periodical.startOnThisNode()) {
                    LOG.info("Not starting [{}] periodical. Not configured to run on this node.", periodical.getClass().getCanonicalName());
                    continue;
                }

                // Register and start.
                periodicals.registerAndStart(periodical);
            } catch (Exception e) {
                LOG.error("Could not initialize periodical.", e);
            }
        }
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);

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
