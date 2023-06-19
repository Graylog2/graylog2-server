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
package org.graylog2.bootstrap.preflight;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class PreflightPeriodicalsService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(PreflightPeriodicalsService.class);

    private final Periodicals periodicals;
    private final Set<Periodical> allPeriodicals;
    private final Set<Periodical> leaderNodePeriodicals = new HashSet<>();
    private final Set<Periodical> anyNodePeriodicals = new HashSet<>();

    @Inject
    public PreflightPeriodicalsService(Periodicals periodicals, Set<Periodical> allPeriodicals) {
        this.periodicals = periodicals;
        this.allPeriodicals = allPeriodicals;

        allPeriodicals.forEach(p -> {
            if (p.leaderOnly()) {
                leaderNodePeriodicals.add(p);
            } else {
                anyNodePeriodicals.add(p);
            }
        });
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting {} periodicals ...", anyNodePeriodicals.size());
        LOG.info("Delaying start of {} periodicals until this node becomes leader ...", leaderNodePeriodicals.size());
        startPeriodicals(anyNodePeriodicals);
    }

    private synchronized void startPeriodicals(Set<Periodical> periodicalsToStart) {
        final Sets.SetView<Periodical> notYetStartedPeriodicals =
                Sets.difference(periodicalsToStart, ImmutableSet.copyOf(periodicals.getAll()));

        int numOfPeriodicalsToSkip = periodicalsToStart.size() - notYetStartedPeriodicals.size();

        if (numOfPeriodicalsToSkip > 0) {
            LOG.warn("Skipping start of {} periodicals which have already been started.", numOfPeriodicalsToSkip);
        }

        for (Periodical periodical : notYetStartedPeriodicals) {
            try {
                periodical.initialize();

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

    private synchronized void stopPeriodicals(Collection<Periodical> periodicalsToStop) {
        periodicalsToStop.forEach(periodicals::unregisterAndStop);
    }

    @Override
    protected void shutDown() throws Exception {
        stopPeriodicals(periodicals.getAllStoppedOnGracefulShutdown());
    }
}
