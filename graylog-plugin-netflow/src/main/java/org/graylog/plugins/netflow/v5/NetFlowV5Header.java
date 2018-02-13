/*
 * Copyright 2013 Eediom Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
