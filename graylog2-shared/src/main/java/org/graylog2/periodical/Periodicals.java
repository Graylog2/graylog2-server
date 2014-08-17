/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

            Thread t = new Thread(periodical);
            t.setDaemon(periodical.isDaemon());
            t.setName("periodical-" + periodical.getClass().getCanonicalName());
            t.setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LOG));
            t.start();
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
