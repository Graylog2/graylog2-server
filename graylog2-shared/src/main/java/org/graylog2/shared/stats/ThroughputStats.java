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
