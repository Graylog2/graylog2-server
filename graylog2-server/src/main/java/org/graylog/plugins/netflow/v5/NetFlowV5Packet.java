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
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
public abstract class NetFlowV5Packet {
    public abstract NetFlowV5Header header();

    public abstract ImmutableList<NetFlowV5Record> records();

    public abstract long dataLength();

    public static NetFlowV5Packet create(NetFlowV5Header header, List<NetFlowV5Record> records, long dataLength) {
        return new AutoValue_NetFlowV5Packet(header, ImmutableList.copyOf(records), dataLength);
    }
}
