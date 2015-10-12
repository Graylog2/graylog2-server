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
