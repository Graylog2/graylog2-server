/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.initializers;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
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

    @Inject
    public PeriodicalsService(Periodicals periodicals,
                              ServerStatus serverStatus,
                              Set<Periodical> periodicalSet) {
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

                if (periodical.primaryOnly() && !serverStatus.hasCapability(ServerStatus.Capability.PRIMARY)) {
                    LOG.info("Not starting [{}] periodical. Only started on Graylog primary nodes.", periodical.getClass().getCanonicalName());
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
