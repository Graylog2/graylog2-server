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
