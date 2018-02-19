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
package org.graylog.plugins.netflow.v5;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NetFlowV5Header {
    // bytes 0-1
    public abstract int version();

    // bytes 2-3
    public abstract int count();

    // bytes 4-7, milliseconds since device boot
    public abstract long sysUptime();

    // bytes 8-11, seconds since UTC 1970
    public abstract long unixSecs();

    // bytes 12-15, nanoseconds since UTC 1970
    public abstract long unixNsecs();

    // bytes 16-19, sequence counter of total flow seen
    public abstract long flowSequence();

    // bytes 20, type of flow switching engine
    public abstract int engineType();

    // bytes 21, slot number of the flow-switching engine
    public abstract int engineId();

    // bytes 22-23, first two bits hold the sampling mode, remaining 14 bits
    // hold value of sampling interval
    public abstract int samplingMode();

    public abstract int samplingInterval();

    static NetFlowV5Header create(int version,
                                  int count,
                                  long sysUptime,
                                  long unixSecs,
                                  long unixNsecs,
                                  long flowSequence,
                                  int engineType,
                                  int engineId,
                                  int samplingMode,
                                  int samplingInterval
    ) {
        return new AutoValue_NetFlowV5Header(
                version,
                count,
                sysUptime,
                unixSecs,
                unixNsecs,
                flowSequence,
                engineType,
                engineId,
                samplingMode,
                samplingInterval);
    }
}
