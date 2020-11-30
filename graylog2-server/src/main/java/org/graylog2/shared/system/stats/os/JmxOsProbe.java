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
package org.graylog2.shared.system.stats.os;

import javax.inject.Singleton;
import java.lang.management.ManagementFactory;

@Singleton
public class JmxOsProbe implements OsProbe {
    @Override
    public OsStats osStats() {
        final long uptime = -1L;
        final double systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        final double[] loadAverage = systemLoadAverage < 0.0d ? new double[0] : new double[]{systemLoadAverage};
        final Processor processor = Processor.create("Unknown", "Unknown", -1, -1, -1, -1, -1L,
                (short) -1, (short) -1, (short) -1, (short) -1);
        final Memory memory = Memory.create(-1L, -1L, (short) -1, -1L, (short) -1, -1L, -1L);
        final Swap swap = Swap.create(-1L, -1L, -1L);

        return OsStats.create(loadAverage, uptime, processor, memory, swap);
    }
}
