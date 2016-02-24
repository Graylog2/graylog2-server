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
package org.graylog2.plugin.inputs.util;

import com.codahale.metrics.Gauge;
import com.google.common.collect.Maps;
import org.jboss.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.jboss.netty.handler.traffic.TrafficCounter;
import org.jboss.netty.util.HashedWheelTimer;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ThroughputCounter extends GlobalTrafficShapingHandler {

    @Inject
    public ThroughputCounter(HashedWheelTimer wheelTimer) {
        super(wheelTimer, 1000);
    }

    public Map<String, Gauge<Long>> gauges() {
        Map<String, Gauge<Long>> gauges = Maps.newHashMap();

        final TrafficCounter tc = this.getTrafficCounter();

        gauges.put("read_bytes_1sec", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.getLastReadBytes();
            }
        });

        gauges.put("written_bytes_1sec", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.getLastWrittenBytes();
            }
        });

        gauges.put("read_bytes_total", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.getCumulativeReadBytes();
            }
        });

        gauges.put("written_bytes_total", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.getCumulativeWrittenBytes();
            }
        });

        return gauges;
    }
}
