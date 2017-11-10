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
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

// TODO: Dedicated scheduled executor
@ChannelHandler.Sharable
public class ThroughputCounter extends GlobalTrafficShapingHandler {
    @Inject
    public ThroughputCounter(EventLoopGroup executor) {
        super(executor, 1000);
    }

    public Map<String, Gauge<Long>> gauges() {
        Map<String, Gauge<Long>> gauges = new HashMap<>();

        final TrafficCounter tc = trafficCounter();

        gauges.put("read_bytes_1sec", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.lastReadBytes();
            }
        });

        gauges.put("written_bytes_1sec", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.lastWrittenBytes();
            }
        });

        gauges.put("read_bytes_total", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.cumulativeReadBytes();
            }
        });

        gauges.put("written_bytes_total", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.cumulativeWrittenBytes();
            }
        });

        return gauges;
    }
}
