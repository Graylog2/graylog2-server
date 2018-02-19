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

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

@AutoValue
public abstract class RawNetFlowV9Packet {

    public abstract NetFlowV9Header header();

    public abstract int dataLength();

    public abstract Map<Integer, byte[]> templates();

    @Nullable
    public abstract Map.Entry<Integer, byte[]> optionTemplate();

    public abstract Set<Integer> usedTemplates();

    public static RawNetFlowV9Packet create(NetFlowV9Header header, int dataLength, Map<Integer, byte[]> templates, @Nullable Map.Entry<Integer, byte[]> optTemplate, Set<Integer> usedTemplates) {
        return new AutoValue_RawNetFlowV9Packet(header, dataLength, templates, optTemplate, usedTemplates);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n");
        sb.append(header().prettyHexDump()).append("\n");
        sb.append("\nTemplates:\n");
        templates().forEach((integer, byteBuf) -> {
            sb.append("\n").append(integer).append(":\n").append(ByteBufUtil.prettyHexDump(Unpooled.wrappedBuffer(byteBuf)));
        });
        final Map.Entry<Integer, byte[]> optionTemplate = optionTemplate();
        if (optionTemplate != null) {
            sb.append("\nOption Template:\n").append(ByteBufUtil.prettyHexDump(Unpooled.wrappedBuffer(optionTemplate.getValue())));
        }
        sb.append("\nData flows using these templates:\n");
        usedTemplates().forEach(templateId -> sb.append(templateId).append(" "));
        return sb.toString();
    }
}
