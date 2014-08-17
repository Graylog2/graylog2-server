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
package org.graylog2.shared.initializers;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.bindings.InstantiationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class PeriodicalsService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicalsService.class);

    public static final String NAME = "Periodicals initializer";
    private final InstantiationService instantiationService;
    private final Periodicals periodicals;
    private final ServerStatus serverStatus;
    private final Set<Periodical> periodicalSet;

    @Inject
    public PeriodicalsService(InstantiationService instantiationService,
                                  Periodicals periodicals,
                                  ServerStatus serverStatus,
                                  Set<Periodical> periodicalSet) {
        this.instantiationService = instantiationService;
        this.periodicals = periodicals;
        this.serverStatus = serverStatus;
        this.periodicalSet = periodicalSet;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting {} periodicals ...", periodicalSet.size());

        for (Periodical periodical : periodicalSet) {
            try {
                periodical.initialize();

                if (periodical.masterOnly() && !serverStatus.hasCapability(ServerStatus.Capability.MASTER)) {
                    LOG.info("Not starting [{}] periodical. Only started on graylog2-server master nodes.", periodical.getClass().getCanonicalName());
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
        for (Periodical periodical : periodicals.getAllStoppedOnGracefulShutdown()) {
            LOG.info("Shutting down periodical [{}].", periodical.getClass().getCanonicalName());
            Stopwatch s = Stopwatch.createStarted();

            // Cancel future executions.
            Map<Periodical,ScheduledFuture> futures = periodicals.getFutures();
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
