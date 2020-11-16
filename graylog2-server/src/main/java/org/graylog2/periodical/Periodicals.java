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
package org.graylog2.periodical;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Periodicals {

    private static final Logger LOG = LoggerFactory.getLogger(Periodicals.class);

    private final List<Periodical> periodicals;
    private final Map<Periodical, ScheduledFuture> futures;
    private final ScheduledExecutorService scheduler;
    private final ScheduledExecutorService daemonScheduler;

    public Periodicals(ScheduledExecutorService scheduler, ScheduledExecutorService daemonScheduler) {
        this.scheduler = scheduler;
        this.daemonScheduler = daemonScheduler;
        this.periodicals = Lists.newArrayList();
        this.futures = Maps.newHashMap();
    }

    public synchronized void registerAndStart(Periodical periodical) {
        if (periodical.runsForever()) {
            LOG.info("Starting [{}] periodical, running forever.", periodical.getClass().getCanonicalName());

            for (int i = 0; i < periodical.getParallelism(); i++) {
                Thread t = new Thread(periodical);
                t.setDaemon(periodical.isDaemon());
                t.setName("periodical-" + periodical.getClass().getCanonicalName() + "-" + i);
                t.setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LOG));
                t.start();
            }
        } else {
            LOG.info(
                    "Starting [{}] periodical in [{}s], polling every [{}s].",
                    periodical.getClass().getCanonicalName(),
                    periodical.getInitialDelaySeconds(),
                    periodical.getPeriodSeconds());

            ScheduledExecutorService scheduler = periodical.isDaemon() ? this.daemonScheduler : this.scheduler;
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                    periodical,
                    periodical.getInitialDelaySeconds(),
                    periodical.getPeriodSeconds(),
                    TimeUnit.SECONDS
            );

            futures.put(periodical, future);
        }

        periodicals.add(periodical);
    }

    /**
     *
     * @return a copy of the list of all registered periodicals.
     */
    public List<Periodical> getAll() {
        return Lists.newArrayList(periodicals);
    }

    /**
     *
     * @return a copy of the list of all registered periodicals that are configured to be
     * stopped on a graceful shutdown.
     */
    public List<Periodical> getAllStoppedOnGracefulShutdown() {
        List<Periodical> result = Lists.newArrayList();
        for (Periodical periodical : periodicals) {
            if (periodical.stopOnGracefulShutdown()) {
                result.add(periodical);
            }
        }

        return result;
    }

    /**
     *
     * @return a copy of the map of all executor futures
     */
    public Map<Periodical, ScheduledFuture> getFutures() {
        return Maps.newHashMap(futures);
    }

}
