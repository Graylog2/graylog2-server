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
package org.graylog.integrations.ipfix.codecs;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.pkts.Pcap;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;
import org.graylog.integrations.ipfix.InformationElementDefinitions;
import org.graylog.integrations.ipfix.IpfixJournal;
import org.graylog.integrations.ipfix.IpfixMessage;
import org.graylog.integrations.ipfix.IpfixParser;
import org.graylog.integrations.ipfix.Utils;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class IpfixAggregatorTest {
    private static final Logger LOG = LoggerFactory.getLogger(IpfixAggregatorTest.class);
    private static final int COLLIDING_TEMPLATE_ID = 258;
    private static final int IE_PROTOCOL_IDENTIFIER = 4;
    private static final int IE_SOURCE_IPV4_ADDRESS = 8;
    private static final int IE_DESTINATION_TRANSPORT_PORT = 11;
    private final InetSocketAddress someAddress = InetSocketAddress.createUnresolved("192.168.1.1", 999);
    private final MessageFactory messageFactory = new TestMessageFactory();

    private InformationElementDefinitions standardDefinition = new InformationElementDefinitions(
            Resources.getResource("ipfix-iana-elements.json")
    );

    @TempDir
    public File temporaryFolder;

    @Disabled("Not ready. Has InvalidIPFixMessageVersion.")
    @Test
    public void completePacket() throws IOException {
        final ByteBuf packetBytes = Utils.readPacket("templates-data.ipfix");

        final IpfixAggregator aggregator = new IpfixAggregator();
        final CodecAggregator.Result result = aggregator.addChunk(packetBytes, someAddress);

        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getMessage()).isNotNull();

        final IpfixMessage ipfixMessage = new IpfixParser(standardDefinition).parseMessage(result.getMessage());
        assertThat(ipfixMessage).isNotNull();
    }

    @Disabled("Not ready. Has InvalidIPFixMessageVersion.")
    @Test
    public void multipleMessagesTemplateLater() throws IOException {
        final ByteBuf datasetOnlyBytes = Utils.readPacket("dataset-only.ipfix");
        final ByteBuf withTemplatesBytes = Utils.readPacket("templates-data.ipfix");
        final IpfixAggregator ipfixAggregator = new IpfixAggregator();
        final CodecAggregator.Result resultQueued = ipfixAggregator.addChunk(datasetOnlyBytes, someAddress);
        assertThat(resultQueued.isValid()).isTrue();
        assertThat(resultQueued.getMessage()).isNull();

        final CodecAggregator.Result resultComplete = ipfixAggregator.addChunk(withTemplatesBytes, someAddress);
        assertThat(resultComplete.isValid()).isTrue();
        assertThat(resultComplete.getMessage()).isNotNull();

        final IpfixMessage ipfixMessage = new IpfixParser(standardDefinition).parseMessage(resultComplete.getMessage());
        assertThat(ipfixMessage.flows()).hasSize(4);
    }

    @Test
    public void dataAndDataTemplate() throws IOException, URISyntaxException {

        final IpfixAggregator ipfixAggregator = new IpfixAggregator();
        final Map<String, Object> configMap = getIxiaConfigmap();
        final Configuration configuration = new Configuration(configMap);

        final IpfixCodec codec = new IpfixCodec(configuration, ipfixAggregator, messageFactory);

        AtomicInteger messageCount = new AtomicInteger();
        try (InputStream stream = Resources.getResource("data-datatemplate.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(stream);
            pcap.loop(packet -> {
                if (packet.hasProtocol(Protocol.UDP)) {
                    final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                    final InetSocketAddress source = new InetSocketAddress(udp.getParentPacket().getSourceIP(), udp.getSourcePort());
                    byte[] payload = new byte[udp.getPayload().getReadableBytes()];
                    udp.getPayload().getBytes(payload);
                    final ByteBuf buf = Unpooled.wrappedBuffer(payload);
                    final CodecAggregator.Result result = ipfixAggregator.addChunk(buf, source);
                    final ByteBuf ipfixRawBuf = result.getMessage();
                    if (ipfixRawBuf != null) {
                        byte[] bytes = new byte[ipfixRawBuf.readableBytes()];
                        ipfixRawBuf.getBytes(0, bytes);
                        final Collection<Message> messages = codec.decodeMessages(new RawMessage(bytes));
                        if (messages != null) {
                            messageCount.addAndGet(messages.size());
                        }
                    }
                }
                return true;
            });
        } catch (IOException e) {
            LOG.debug("Cannot process PCAP stream", e);
        }
        assertThat(messageCount.get()).isEqualTo(4L);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ixFlowTest() throws IOException, URISyntaxException {
        final IpfixAggregator ipfixAggregator = new IpfixAggregator();
        final Map<String, Object> configMap = getIxiaConfigmap();
        final IpfixCodec codec = new IpfixCodec(new Configuration(configMap), ipfixAggregator, messageFactory);
        final List<Message> messages = new ArrayList<>();

        // ixflow.pcap contains 4 packets, the first has the data templates and option templates
        // followed by three data sets. two sets have subtemplateList data, the third has only empty lists for domain information
        try (InputStream stream = Resources.getResource("ixflow.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(stream);
            pcap.loop(packet -> {
                if (packet.hasProtocol(Protocol.UDP)) {
                    final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                    final InetSocketAddress source = new InetSocketAddress(udp.getParentPacket().getSourceIP(), udp.getSourcePort());
                    byte[] payload = new byte[udp.getPayload().getReadableBytes()];
                    udp.getPayload().getBytes(payload);
                    final ByteBuf buf = Unpooled.wrappedBuffer(payload);
                    final CodecAggregator.Result result = ipfixAggregator.addChunk(buf, source);
                    final ByteBuf ipfixRawBuf = result.getMessage();
                    if (ipfixRawBuf != null) {
                        byte[] bytes = new byte[ipfixRawBuf.readableBytes()];
                        ipfixRawBuf.getBytes(0, bytes);
                        messages.addAll(Objects.requireNonNull(codec.decodeMessages(new RawMessage(bytes))));
                    }
                }
                return true;
            });
        } catch (IOException e) {
            fail("Cannot process PCAP stream");
        }

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getFields())
                .doesNotContainKey("httpSession")
                .containsEntry("dnsRecord_0_dnsIpv4Address", "1.2.0.2")
                .containsEntry("dnsRecord_0_dnsIpv6Address", "0:0:0:0:0:0:0:0")
                .containsEntry("dnsRecord_0_dnsName", "server-1020002.example.int.");
        assertThat(messages.get(1).getFields())
                .doesNotContainKey("httpSession")
                .containsEntry("dnsRecord_0_dnsIpv4Address", "1.2.14.73")
                .containsEntry("dnsRecord_0_dnsIpv6Address", "0:0:0:0:0:0:0:0")
                .containsEntry("dnsRecord_0_dnsName", "server-1020e49.example.int.");
        assertThat(messages.get(2).getFields())
                .doesNotContainKey("httpSession")
                .doesNotContainKey("dnsRecord");

    }

    @Test
    public void ignoreTrailingRecordPadding() throws IOException {
        final IpfixAggregator ipfixAggregator = new IpfixAggregator();
        final IpfixCodec codec = new IpfixCodec(new Configuration(Map.of()), ipfixAggregator, messageFactory);
        final List<Message> messages = new ArrayList<>();

        try (InputStream stream = Resources.getResource("trailingpadding.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(stream);
            pcap.loop(packet -> {
                if (packet.hasProtocol(Protocol.UDP)) {
                    final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                    final InetSocketAddress source = new InetSocketAddress(udp.getParentPacket().getSourceIP(), udp.getSourcePort());
                    byte[] payload = new byte[udp.getPayload().getReadableBytes()];
                    udp.getPayload().getBytes(payload);
                    final ByteBuf buf = Unpooled.wrappedBuffer(payload);
                    final CodecAggregator.Result result = ipfixAggregator.addChunk(buf, source);
                    final ByteBuf ipfixRawBuf = result.getMessage();
                    if (ipfixRawBuf != null) {
                        byte[] bytes = new byte[ipfixRawBuf.readableBytes()];
                        ipfixRawBuf.getBytes(0, bytes);
                        messages.addAll(Objects.requireNonNull(codec.decodeMessages(new RawMessage(bytes))));
                    }
                }
                return true;
            });
        } catch (IOException e) {
            fail("Cannot process PCAP stream");
        }
        assertThat(messages).hasSize(2);
    }

    @Test
    public void usesOnlyTemplatesOfTheSendingExporterWhenTemplateIdsCollide() throws IOException {
        // Two exporters use the same observation domain id and the same template id, but different field layouts.
        final Exporter exporterA = new Exporter(InetSocketAddress.createUnresolved("192.168.1.1", 4739),
                templateRecord(COLLIDING_TEMPLATE_ID, IE_SOURCE_IPV4_ADDRESS, 4, IE_PROTOCOL_IDENTIFIER, 1),
                new byte[]{10, 0, 0, 1, 6},
                "10.0.0.1", 6L);
        final Exporter exporterB = new Exporter(InetSocketAddress.createUnresolved("192.168.1.2", 4739),
                templateRecord(COLLIDING_TEMPLATE_ID, IE_SOURCE_IPV4_ADDRESS, 4, IE_DESTINATION_TRANSPORT_PORT, 2, IE_PROTOCOL_IDENTIFIER, 1),
                new byte[]{10, 0, 0, 2, 0x01, (byte) 0xbb, 17},
                "10.0.0.2", 17L);

        // the arrival order of the exporters must not matter
        assertExportersUseTheirOwnTemplates(exporterA, exporterB);
        assertExportersUseTheirOwnTemplates(exporterB, exporterA);
    }

    private void assertExportersUseTheirOwnTemplates(Exporter first, Exporter second) throws IOException {
        final IpfixAggregator ipfixAggregator = new IpfixAggregator();
        final IpfixCodec codec = new IpfixCodec(new Configuration(Map.of()), ipfixAggregator, messageFactory);

        // template and data record in the same packet
        for (Exporter exporter : List.of(first, second)) {
            assertAggregatedWithOwnTemplate(ipfixAggregator, codec, exporter, ipfixPacket(exporter.templateRecord(), exporter.dataRecord()));
        }
        // data-only packets after the templates of both exporters are known
        for (Exporter exporter : List.of(first, second)) {
            assertAggregatedWithOwnTemplate(ipfixAggregator, codec, exporter, ipfixPacket(null, exporter.dataRecord()));
        }
    }

    private void assertAggregatedWithOwnTemplate(IpfixAggregator ipfixAggregator, IpfixCodec codec, Exporter exporter, ByteBuf packet) throws IOException {
        final CodecAggregator.Result result = ipfixAggregator.addChunk(packet, exporter.address());
        assertThat(result.isValid()).isTrue();
        assertThat(result.getMessage()).isNotNull();

        final byte[] bytes = ByteBufUtil.getBytes(result.getMessage());
        final IpfixJournal.RawIpfix rawIpfix = IpfixJournal.RawIpfix.parseFrom(bytes);
        assertThat(rawIpfix.getTemplatesMap())
                .as("templates sent along with the data of exporter %s", exporter.address())
                .containsOnlyKeys(COLLIDING_TEMPLATE_ID)
                .containsEntry(COLLIDING_TEMPLATE_ID, ByteString.copyFrom(exporter.templateRecord()));

        final Collection<Message> messages = codec.decodeMessages(new RawMessage(bytes));
        assertThat(messages).singleElement().satisfies(message -> {
            assertThat(message.getField("sourceIPv4Address")).isEqualTo(exporter.expectedSourceAddress());
            assertThat(message.getField("protocolIdentifier")).isEqualTo(exporter.expectedProtocol());
        });
    }

    private static byte[] templateRecord(int templateId, int... fieldIdsAndLengths) {
        final ByteBuf buf = Unpooled.buffer();
        buf.writeShort(templateId);
        buf.writeShort(fieldIdsAndLengths.length / 2);
        for (int value : fieldIdsAndLengths) {
            buf.writeShort(value);
        }
        return ByteBufUtil.getBytes(buf);
    }

    private static ByteBuf ipfixPacket(@Nullable byte[] templateRecord, byte[] dataRecord) {
        final ByteBuf sets = Unpooled.buffer();
        if (templateRecord != null) {
            sets.writeShort(2); // template set id
            sets.writeShort(4 + templateRecord.length);
            sets.writeBytes(templateRecord);
        }
        sets.writeShort(COLLIDING_TEMPLATE_ID); // data set id
        sets.writeShort(4 + dataRecord.length);
        sets.writeBytes(dataRecord);

        final ByteBuf packet = Unpooled.buffer();
        packet.writeShort(10); // IPFIX version
        packet.writeShort(16 + sets.readableBytes());
        packet.writeInt(1700000000); // export time
        packet.writeInt(1); // sequence number
        packet.writeInt(0); // observation domain id
        packet.writeBytes(sets);
        return packet;
    }

    private record Exporter(InetSocketAddress address, byte[] templateRecord, byte[] dataRecord,
                            String expectedSourceAddress, long expectedProtocol) {
    }

    private Map<String, Object> getIxiaConfigmap() throws URISyntaxException {
        final File filePath = new File(Resources.getResource("ixia-ied.json").toURI());
        final Map<String, Object> configMap = Maps.newHashMap();
        configMap.put(IpfixCodec.CK_IPFIX_DEFINITION_PATH, Collections.singletonList(filePath.getAbsolutePath()));
        return configMap;
    }

}
