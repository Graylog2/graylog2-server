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
