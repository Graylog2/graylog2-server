/**
 * Copyright (C) 2012, 2013, 2014 wasted.io Ltd <really@wasted.io>
 * Copyright (C) 2015 Graylog, Inc. (hello@graylog.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.netflow.utils;

import com.google.common.net.InetAddresses;
import io.netty.buffer.ByteBuf;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ByteBufUtils {
    public static final InetAddress DEFAULT_INET_ADDRESS = InetAddresses.forString("0.0.0.0");

    public static long getUnsignedInteger(final ByteBuf buf, final int offset, final int length) {
        switch (length) {
            case 1: return buf.getUnsignedByte(offset);
            case 2: return buf.getUnsignedShort(offset);
            case 3: return buf.getUnsignedMedium(offset);
            case 4: return buf.getUnsignedInt(offset);
            case 8: return buf.getLong(offset) & 0x00000000ffffffffL;
            default: return 0L;
        }
    }
    public static InetAddress getInetAddress(final ByteBuf buf, final int offset, final int length) {
        final ByteBuf buffer = buf.slice(offset, length);

        final byte[] data = new byte[length];
        for (int i = 1; i <= length; i++) {
            data[i - 1] = (byte) buffer.readUnsignedByte();
        }

        InetAddress address;
        try {
            address = InetAddress.getByAddress(data);
        } catch (UnknownHostException e) {
            address = DEFAULT_INET_ADDRESS;
        }

        return address;
    }
}
