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
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
public abstract class NetFlowV9Packet {
    public abstract NetFlowV9Header header();

    public abstract ImmutableList<NetFlowV9Template> templates();

    @Nullable
    public abstract NetFlowV9OptionTemplate optionTemplate();

    public abstract ImmutableList<NetFlowV9BaseRecord> records();

    public abstract long dataLength();

    public static NetFlowV9Packet create(NetFlowV9Header header,
                                         List<NetFlowV9Template> templates,
                                         @Nullable NetFlowV9OptionTemplate optionTemplate,
                                         List<NetFlowV9BaseRecord> records,
                                         long dataLength) {
        return new AutoValue_NetFlowV9Packet(header, ImmutableList.copyOf(templates), optionTemplate, ImmutableList.copyOf(records), dataLength);
    }

    @Override
    public String toString() {
        return header().toString();
    }
}
