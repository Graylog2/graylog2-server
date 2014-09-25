/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
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
