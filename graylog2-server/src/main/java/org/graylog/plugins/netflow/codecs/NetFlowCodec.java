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
package org.graylog.plugins.netflow.codecs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.graylog.plugins.netflow.flows.FlowException;
import org.graylog.plugins.netflow.flows.NetFlowFormatter;
import org.graylog.plugins.netflow.v5.NetFlowV5Packet;
import org.graylog.plugins.netflow.v5.NetFlowV5Parser;
import org.graylog.plugins.netflow.v9.NetFlowV9FieldTypeRegistry;
import org.graylog.plugins.netflow.v9.NetFlowV9Journal;
import org.graylog.plugins.netflow.v9.NetFlowV9OptionTemplate;
import org.graylog.plugins.netflow.v9.NetFlowV9Packet;
import org.graylog.plugins.netflow.v9.NetFlowV9Parser;
import org.graylog.plugins.netflow.v9.NetFlowV9Record;
import org.graylog.plugins.netflow.v9.NetFlowV9Template;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Codec(name = "netflow", displayName = "NetFlow")
public class NetFlowCodec extends AbstractCodec implements MultiMessageCodec {
    /**
     * Marker byte which signals that the contained netflow packet should be parsed as is.
     */
    public static final byte PASSTHROUGH_MARKER = 0x00;
    /**
     * Marker byte which signals that the contained netflow v9 packet is non-RFC:
     * It contains all necessary template flows before any data flows and can be completely parsed without a template cache.
     */
    public static final byte ORDERED_V9_MARKER = 0x01;
    @VisibleForTesting
    static final String CK_NETFLOW9_DEFINITION_PATH = "netflow9_definitions_Path";
    private static final Logger LOG = LoggerFactory.getLogger(NetFlowCodec.class);
    private final NetFlowV9FieldTypeRegistry typeRegistry;
    private final NetflowV9CodecAggregator netflowV9CodecAggregator;

    @Inject
    protected NetFlowCodec(@Assisted Configuration configuration, NetflowV9CodecAggregator netflowV9CodecAggregator) throws IOException {
        super(configuration);
        this.netflowV9CodecAggregator = netflowV9CodecAggregator;

        final String netFlow9DefinitionsPath = configuration.getString(CK_NETFLOW9_DEFINITION_PATH);
        if (netFlow9DefinitionsPath == null || netFlow9DefinitionsPath.trim().isEmpty()) {
            this.typeRegistry = NetFlowV9FieldTypeRegistry.create();
        } else {
            try (InputStream inputStream = new FileInputStream(netFlow9DefinitionsPath)) {
                this.typeRegistry = NetFlowV9FieldTypeRegistry.create(inputStream);
            }
        }
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return netflowV9CodecAggregator;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        throw new UnsupportedOperationException("MultiMessageCodec " + getClass() + " does not support decode()");
    }

    @Nullable
    @Override
    public Collection<Message> decodeMessages(@Nonnull RawMessage rawMessage) {
        try {
            final ResolvableInetSocketAddress remoteAddress = rawMessage.getRemoteAddress();
            final InetSocketAddress sender = remoteAddress != null ? remoteAddress.getInetSocketAddress() : null;

            final byte[] payload = rawMessage.getPayload();
            if (payload.length < 3) {
                LOG.debug("NetFlow message (source: {}) doesn't even fit the NetFlow version (size: {} bytes)",
                        sender, payload.length);
                return null;
            }

            final ByteBuf buffer = Unpooled.wrappedBuffer(payload);
            switch (buffer.readByte()) {
                case PASSTHROUGH_MARKER:
                    final NetFlowV5Packet netFlowV5Packet = NetFlowV5Parser.parsePacket(buffer);

                    return netFlowV5Packet.records().stream()
                            .map(record -> NetFlowFormatter.toMessage(netFlowV5Packet.header(), record, sender))
                            .collect(Collectors.toList());
                case ORDERED_V9_MARKER:
                    // our "custom" netflow v9 that has all the templates in the same packet
                    return decodeV9(sender, buffer);
                default:
                    final List<RawMessage.SourceNode> sourceNodes = rawMessage.getSourceNodes();
                    final RawMessage.SourceNode sourceNode = sourceNodes.isEmpty() ? null : sourceNodes.get(sourceNodes.size() - 1);
                    final String inputId = sourceNode == null ? "<unknown>" : sourceNode.inputId;
                    LOG.warn("Unsupported NetFlow packet on input {} (source: {})", inputId, sender);
                    return null;
            }
        } catch (FlowException e) {
            LOG.error("Error parsing NetFlow packet <{}> received from <{}>", rawMessage.getId(), rawMessage.getRemoteAddress(), e);
            if (LOG.isDebugEnabled()) {
                LOG.debug("NetFlow packet hexdump:\n{}", ByteBufUtil.prettyHexDump(Unpooled.wrappedBuffer(rawMessage.getPayload())));
            }
            return null;
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Invalid NetFlowV9 entry found, cannot parse the messages", ExceptionUtils.getRootCause(e));
            return null;
        }
    }

    @VisibleForTesting
    Collection<Message> decodeV9(InetSocketAddress sender, ByteBuf buffer) throws InvalidProtocolBufferException {
        final List<NetFlowV9Packet> netFlowV9Packets = decodeV9Packets(buffer);

        return netFlowV9Packets.stream().map(netFlowV9Packet -> netFlowV9Packet.records().stream()
                .filter(record -> record instanceof NetFlowV9Record)
                .map(record -> NetFlowFormatter.toMessage(netFlowV9Packet.header(), record, sender))
                .collect(Collectors.toList())
        ).flatMap(Collection::stream)
         .collect(Collectors.toList());
    }

    @VisibleForTesting
    List<NetFlowV9Packet> decodeV9Packets(ByteBuf buffer) throws InvalidProtocolBufferException {
        byte[] v9JournalEntry = new byte[buffer.readableBytes()];
        buffer.readBytes(v9JournalEntry);
        final NetFlowV9Journal.RawNetflowV9 rawNetflowV9 = NetFlowV9Journal.RawNetflowV9.parseFrom(v9JournalEntry);

        // parse all templates used in the packet
        final Map<Integer, NetFlowV9Template> templateMap = Maps.newHashMap();
        rawNetflowV9.getTemplatesMap().forEach((templateId, byteString) -> {
            final NetFlowV9Template netFlowV9Template = NetFlowV9Parser.parseTemplate(
                    Unpooled.wrappedBuffer(byteString.toByteArray()), typeRegistry);
            templateMap.put(templateId, netFlowV9Template);
        });
        final NetFlowV9OptionTemplate[] optionTemplate = {null};
        rawNetflowV9.getOptionTemplateMap().forEach((templateId, byteString) -> {
            optionTemplate[0] = NetFlowV9Parser.parseOptionTemplate(Unpooled.wrappedBuffer(byteString.toByteArray()), typeRegistry);
        });

        return rawNetflowV9.getPacketsList().stream()
                .map(bytes -> Unpooled.wrappedBuffer(bytes.toByteArray()))
                .map(buf -> NetFlowV9Parser.parsePacket(buf, typeRegistry, templateMap, optionTemplate[0]))
                .collect(Collectors.toList());
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<NetFlowCodec> {
        @Override
        NetFlowCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(2055);
            }
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configuration = super.getRequestedConfiguration();
            configuration.addField(new TextField(CK_NETFLOW9_DEFINITION_PATH, "Netflow 9 field definitions", "", "Path to the YAML file containing Netflow 9 field definitions", ConfigurationField.Optional.OPTIONAL));
            return configuration;
        }
    }
}
