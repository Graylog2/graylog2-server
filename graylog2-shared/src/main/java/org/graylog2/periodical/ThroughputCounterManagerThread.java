/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

import com.google.inject.Inject;
import org.graylog2.shared.stats.ThroughputStats;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ThroughputCounterManagerThread extends Periodical {
    private final ThroughputStats throughputStats;

    @Inject
    public ThroughputCounterManagerThread(ThroughputStats throughputStats) {
        this.throughputStats = throughputStats;
    }

    @Override
    public void run() {
        throughputStats.setCurrentThroughput(throughputStats.getThroughputCounter().get());
        throughputStats.getThroughputCounter().set(0);
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
