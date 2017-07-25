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
package org.graylog.plugins.netflow.v9;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NetFlowV9Header {
    // 2bytes, 9
    public abstract int version();

    // 2bytes, both template and flow count
    public abstract int count();

    // 4bytes
    public abstract long sysUptime();

    // 4bytes, seconds since 0000 Coordinated Universal Time (UTC) 1970
    public abstract long unixSecs();

    // 4bytes, Incremental sequence counter of all export packets sent by this
    // export device(); this value is cumulative, and it can be used to identify
    // whether any export packets have been missed
    public abstract long sequence();

    // 4bytes
    public abstract long sourceId();

    public static NetFlowV9Header create(int version, int count, long sysUptime, long unixSecs, long sequence, long sourceId) {
        return new AutoValue_NetFlowV9Header(version, count, sysUptime, unixSecs, sequence, sourceId);
    }
}
