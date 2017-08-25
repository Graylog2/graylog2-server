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

    public abstract Map<Integer, ByteBuf> templates();

    @Nullable
    public abstract Map.Entry<Integer, ByteBuf> optionTemplate();

    public abstract Set<Integer> usedTemplates();

    public static RawNetFlowV9Packet create(NetFlowV9Header header, int dataLength, Map<Integer, ByteBuf> templates, @Nullable Map.Entry<Integer, ByteBuf> optTemplate, Set<Integer> usedTemplates) {
        return new AutoValue_RawNetFlowV9Packet(header, dataLength, templates, optTemplate, usedTemplates);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n");
        sb.append(ByteBufUtil.prettyHexDump(Unpooled.wrappedBuffer(header().encode().toByteBuffer()))).append("\n");
        sb.append("\nTemplates:\n");
        templates().forEach((integer, byteBuf) -> {
            sb.append("\n").append(integer).append(":\n").append(ByteBufUtil.prettyHexDump(byteBuf));
        });
        final Map.Entry<Integer, ByteBuf> optionTemplate = optionTemplate();
        if (optionTemplate != null) {
            sb.append("\nOption Template:\n").append(ByteBufUtil.prettyHexDump(optionTemplate.getValue()));
        }
        sb.append("\nData flows using these templates:\n");
        usedTemplates().forEach(templateId -> sb.append(templateId).append(" "));
        return sb.toString();
    }
}
