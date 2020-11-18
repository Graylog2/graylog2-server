/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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

// TODO: Dedicated scheduled executor
@ChannelHandler.Sharable
public class ThroughputCounter extends GlobalTrafficShapingHandler {
    public static final String READ_BYTES_1_SEC = "read_bytes_1sec";
    public static final String WRITTEN_BYTES_1_SEC = "written_bytes_1sec";
    public static final String READ_BYTES_TOTAL = "read_bytes_total";
    public static final String WRITTEN_BYTES_TOTAL = "written_bytes_total";

    @Inject
    public ThroughputCounter(EventLoopGroup executor) {
        super(executor, 1000);
    }

    public Map<String, Gauge<Long>> gauges() {
        Map<String, Gauge<Long>> gauges = new HashMap<>();

        final TrafficCounter tc = trafficCounter();

        gauges.put(READ_BYTES_1_SEC, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.lastReadBytes();
            }
        });
        gauges.put(WRITTEN_BYTES_1_SEC, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.lastWrittenBytes();
            }
        });
        gauges.put(READ_BYTES_TOTAL, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.cumulativeReadBytes();
            }
        });
        gauges.put(WRITTEN_BYTES_TOTAL, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tc.cumulativeWrittenBytes();
            }
        });

        return gauges;
    }
}
