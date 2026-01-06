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
package org.graylog2.periodical;

import com.codahale.metrics.Gauge;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CpuLoadGauge implements Gauge<Double> {

    private long[] lastTicks = processor().getSystemCpuLoadTicks();
    private Double cpuLoad;

    @Override
    public Double getValue() {
        return cpuLoad;
    }

    public void update() {
        final CentralProcessor processor = processor();
        final long[] newTicks = processor.getSystemCpuLoadTicks();
        cpuLoad = processor.getSystemCpuLoadBetweenTicks(lastTicks, newTicks) * 100.0d;
        lastTicks = newTicks;
    }

    private static CentralProcessor processor() {
        SystemInfo si = new SystemInfo();
        return si.getHardware().getProcessor();
    }
}
