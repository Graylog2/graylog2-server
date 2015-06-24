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
package org.graylog2.shared.stats;

import org.cliffc.high_scale_lib.Counter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Deprecated
public class ThroughputStats {
    private final AtomicReference<ConcurrentHashMap<String, Counter>> streamThroughput;
    private final AtomicReference<HashMap<String, Counter>> currentStreamThroughput;


    public ThroughputStats() {
        this.streamThroughput = new AtomicReference<>(new ConcurrentHashMap<String, Counter>());
        this.currentStreamThroughput =  new AtomicReference<>();
    }

    public Map<String, Counter> cycleStreamThroughput() {
        return streamThroughput.getAndSet(new ConcurrentHashMap<String, Counter>());
    }

    public void incrementStreamThroughput(String streamId) {
        final ConcurrentHashMap<String, Counter> counterMap = streamThroughput.get();
        counterMap.putIfAbsent(streamId, new Counter());
        counterMap.get(streamId).increment();
    }

    public void setCurrentStreamThroughput(HashMap<String, Counter> throughput) {
        currentStreamThroughput.set(throughput);
    }

    public HashMap<String, Counter> getCurrentStreamThroughput() {
        return currentStreamThroughput.get();
    }

}
