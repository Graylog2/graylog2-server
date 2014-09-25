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
package org.graylog2.shared.stats;

import com.google.common.collect.Maps;
import org.cliffc.high_scale_lib.Counter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ThroughputStats {
    private long currentThroughput;
    private final Counter throughputCounter;
    private final Counter benchmarkCounter;
    private final AtomicReference<ConcurrentHashMap<String, Counter>> streamThroughput;
    private final AtomicReference<HashMap<String, Counter>> currentStreamThroughput;


    public ThroughputStats() {
        this.currentThroughput = 0;
        this.throughputCounter = new Counter();
        this.benchmarkCounter = new Counter();
        this.streamThroughput = new AtomicReference<ConcurrentHashMap<String, Counter>>(new ConcurrentHashMap<String, Counter>());
        this.currentStreamThroughput =  new AtomicReference<HashMap<String, Counter>>();

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

    public AtomicReference<ConcurrentHashMap<String, Counter>> getStreamThroughput() {
        return streamThroughput;
    }

    public Map<String, Counter> cycleStreamThroughput() {
        return streamThroughput.getAndSet(new ConcurrentHashMap<String, Counter>());
    }

    public void incrementStreamThroughput(String streamId) {
        final ConcurrentHashMap<String, Counter> counterMap = streamThroughput.get();
        Counter counter;
        synchronized (counterMap) {
            counter = counterMap.get(streamId);
            if (counter == null) {
                counter = new Counter();
                counterMap.put(streamId, counter);
            }
        }
        counter.increment();
    }

    public void setCurrentStreamThroughput(HashMap<String, Counter> throughput) {
        currentStreamThroughput.set(throughput);
    }

    public HashMap<String, Counter> getCurrentStreamThroughput() {
        return currentStreamThroughput.get();
    }

    public Map<String, Long> getCurrentStreamThroughputValues() {
        Map<String, Long> values = Maps.newHashMap();
        for (Map.Entry<String, Counter> counter : currentStreamThroughput.get().entrySet()) {
            values.put(counter.getKey(), counter.getValue().longValue());
        }

        return values;
    }
}
