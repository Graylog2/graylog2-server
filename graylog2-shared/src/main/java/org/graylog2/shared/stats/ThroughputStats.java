/*
 * Copyright 2013-2014 TORCH GmbH
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
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.shared.stats;

import org.cliffc.high_scale_lib.Counter;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ThroughputStats {
    private long currentThroughput;
    private final Counter throughputCounter;
    private final Counter benchmarkCounter;

    public ThroughputStats() {
        this.currentThroughput = 0;
        this.throughputCounter = new Counter();
        this.benchmarkCounter = new Counter();
    }

    public long getCurrentThroughput() {
        return currentThroughput;
    }

    public Counter getThroughputCounter() {
        return throughputCounter;
    }

    public Counter getBenchmarkCounter() {
        return benchmarkCounter;
    }

    public void setCurrentThroughput(long currentThroughput) {
        this.currentThroughput = currentThroughput;
    }
}
