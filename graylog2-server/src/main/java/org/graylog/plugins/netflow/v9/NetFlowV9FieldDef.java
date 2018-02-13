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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import io.netty.buffer.ByteBuf;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@JsonAutoDetect
@AutoValue
public abstract class NetFlowV9FieldDef {
    @JsonProperty("type")
    public abstract NetFlowV9FieldType type();

    @JsonProperty("length")
    public abstract int length();

    @JsonCreator
    public static NetFlowV9FieldDef create(@JsonProperty("type") NetFlowV9FieldType type, @JsonProperty("length") int length) {
        return new AutoValue_NetFlowV9FieldDef(type, length);
    }

    public Optional<Object> parse(ByteBuf bb) {
        int len = length() != 0 ? length() : type().valueType().getDefaultLength();
        switch (type().valueType()) {
            case UINT8:
            case UINT16:
            case UINT24:
            case UINT32:
            case UINT64:
                return parseUnsignedNumber(bb, len);
            case INT8:
                return Optional.of(bb.readByte());
            case INT16:
                return Optional.of(bb.readShort());
            case INT24:
                return Optional.of(bb.readMedium());
            case INT32:
                return Optional.of(bb.readInt());
            case INT64:
                return Optional.of(bb.readLong());
            case IPV4:
                byte[] b = new byte[4];
                bb.readBytes(b);
                try {
                    return Optional.of(InetAddress.getByAddress(b).getHostAddress());
                } catch (UnknownHostException e) {
                    return Optional.empty();
                }
            case IPV6:
                byte[] b2 = new byte[16];
                bb.readBytes(b2);
                try {
                    return Optional.of(InetAddress.getByAddress(b2).getHostAddress());
                } catch (UnknownHostException e) {
                    return Optional.empty();
                }
            case MAC:
                byte[] b3 = new byte[6];
                bb.readBytes(b3);
                return Optional.of(String.format(Locale.ROOT, "%02x:%02x:%02x:%02x:%02x:%02x", b3[0], b3[1], b3[2], b3[3], b3[4], b3[5]));
            case STRING:
                byte[] b4 = new byte[len];
                bb.readBytes(b4);
                return Optional.of(new String(b4, StandardCharsets.UTF_8));
            default:
                return Optional.empty();
        }
    }

    private Optional<Object> parseUnsignedNumber(ByteBuf bb, int length) {
        switch (length) {
            case 1:
                return Optional.of(bb.readUnsignedByte());
            case 2:
                return Optional.of(bb.readUnsignedShort());
            case 3:
                return Optional.of(bb.readUnsignedMedium());
            case 4:
                return Optional.of(bb.readUnsignedInt());
            case 8:
                return Optional.of(bb.readLong());
            default:
                byte[] uint64Bytes = new byte[length];
                bb.readBytes(uint64Bytes);
                return Optional.of(new BigInteger(uint64Bytes));
        }

    }
}
