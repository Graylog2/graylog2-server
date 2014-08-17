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
package org.graylog2.periodical;

import org.cliffc.high_scale_lib.Counter;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.stats.ThroughputStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class StreamThroughputCounterManagerThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(StreamThroughputCounterManagerThread.class);
    private final ThroughputStats throughputStats;

    @Inject
    public StreamThroughputCounterManagerThread(ThroughputStats throughputStats) {
        this.throughputStats = throughputStats;
    }

    @Override
    public void doRun() {
        // cycleStreamThroughput clears the map already.
        final Map<String,Counter> stringCounterMap = throughputStats.cycleStreamThroughput();
        throughputStats.setCurrentStreamThroughput(new HashMap<>(stringCounterMap));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return false;
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
        return 1;
    }
}
