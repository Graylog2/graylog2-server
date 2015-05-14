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
package org.graylog2.periodical;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.primitives.Ints;
import org.graylog2.Configuration;
import org.graylog2.collectors.CollectorService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PurgeExpiredCollectorsThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(PurgeExpiredCollectorsThread.class);
    private final CollectorService collectorService;
    private final Configuration configuration;

    @Inject
    public PurgeExpiredCollectorsThread(CollectorService collectorService,
                                        Configuration configuration) {
        this.collectorService = collectorService;
        this.configuration = configuration;
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
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 60*60;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Duration threshold = configuration.getCollectorExpirationThreshold();
        final int purgedCollectors = collectorService.destroyExpired(Ints.checkedCast(threshold.getQuantity()), threshold.getUnit());
        LOG.debug("Purged {} inactive collectors.", purgedCollectors);
    }
}
