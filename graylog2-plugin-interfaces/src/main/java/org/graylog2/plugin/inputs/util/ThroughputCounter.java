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
