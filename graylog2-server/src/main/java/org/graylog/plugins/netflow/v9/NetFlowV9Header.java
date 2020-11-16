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
package org.graylog.plugins.netflow.v9;

import com.google.auto.value.AutoValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

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

    public String prettyHexDump() {
        final ByteBuf buffer = Unpooled.buffer(20);
        try {
            buffer.writeShort(version());
            buffer.writeShort(count());
            buffer.writeInt(Math.toIntExact(sysUptime()));
            buffer.writeInt(Math.toIntExact(unixSecs()));
            buffer.writeInt(Math.toIntExact(sequence()));
            buffer.writeInt(Math.toIntExact(sourceId()));

            return ByteBufUtil.prettyHexDump(buffer);
        } finally {
            ReferenceCountUtil.release(buffer);
        }
    }
}
