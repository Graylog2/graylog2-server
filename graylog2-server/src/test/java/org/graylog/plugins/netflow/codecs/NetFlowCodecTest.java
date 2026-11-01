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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.graylog.plugins.netflow.flows.FlowException;
import org.graylog.plugins.netflow.flows.NetFlowFormatter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NetFlowCodecTest {
    @TempDir
    public File temporaryFolder;

    private NetFlowCodec codec;
    private NetflowV9CodecAggregator codecAggregator;
    private final MessageFactory messageFactory = new TestMessageFactory();
    private final NetFlowFormatter netFlowFormatter = new NetFlowFormatter(messageFactory);

    @BeforeEach
    public void setUp() throws Exception {
        codecAggregator = new NetflowV9CodecAggregator();
        codec = new NetFlowCodec(Configuration.EMPTY_CONFIGURATION, codecAggregator, netFlowFormatter);
    }

    @Test
    public void constructorFailsIfNetFlow9DefinitionsPathDoesNotExist() throws Exception {
        final File definitionsFile = File.createTempFile("junit", null, temporaryFolder);
        assertThat(definitionsFile.delete()).isTrue();

        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                NetFlowCodec.CK_NETFLOW9_DEFINITION_PATH, definitionsFile.getAbsolutePath());
        final Configuration configuration = new Configuration(configMap);

        assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> new NetFlowCodec(configuration, codecAggregator, netFlowFormatter))
                .withMessageEndingWith("(No such file or directory)");
    }

    @Test
    public void constructorSucceedsIfNetFlow9DefinitionsPathIsEmpty() throws Exception {
        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                NetFlowCodec.CK_NETFLOW9_DEFINITION_PATH, "");
        final Configuration configuration = new Configuration(configMap);

        assertThat(new NetFlowCodec(configuration, codecAggregator, netFlowFormatter)).isNotNull();
    }

    @Test
    public void constructorSucceedsIfNetFlow9DefinitionsPathIsBlank() throws Exception {
        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                NetFlowCodec.CK_NETFLOW9_DEFINITION_PATH, "   ");
        final Configuration configuration = new Configuration(configMap);

        assertThat(new NetFlowCodec(configuration, codecAggregator, netFlowFormatter)).isNotNull();
    }

    @Test
    public void constructorFailsIfNetFlow9DefinitionsPathIsInvalidYaml() throws Exception {
        final File definitionsFile = File.createTempFile("junit", null, temporaryFolder);
        Files.write(definitionsFile.toPath(), "foo: %bar".getBytes(StandardCharsets.UTF_8));

        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                NetFlowCodec.CK_NETFLOW9_DEFINITION_PATH, definitionsFile.getAbsolutePath());
        final Configuration configuration = new Configuration(configMap);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NetFlowCodec(configuration, codecAggregator, netFlowFormatter))
                .withMessageMatching("Unable to parse NetFlow 9 definitions");
    }

    @Test
    public void decodeThrowsUnsupportedOperationException() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> codec.decodeSafe(new RawMessage(new byte[0])))
                .withMessage("MultiMessageCodec " + NetFlowCodec.class + " does not support decode()");
    }

    @Test
    public void decodeMessagesReturnsNullIfMessageWasInvalid() throws Exception {
        final byte[] b = "Foobar".getBytes(StandardCharsets.UTF_8);
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);
        final RawMessage rawMessage = new RawMessage(b, source);

        assertThatThrownBy(() -> codec.decodeMessages(rawMessage)).isInstanceOf(InputProcessingException.class);
    }

    @Test
    public void decodeMessagesReturnsNullIfNetFlowParserThrowsFlowException() throws Exception {
        final byte[] b = "Foobar".getBytes(StandardCharsets.UTF_8);
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);
        final RawMessage rawMessage = new RawMessage(b, source) {
            private boolean triggered = false;
            @Override
            public byte[] getPayload() {
                if (triggered) {
                    return new byte[]{};
                }
                triggered = true;
                throw new FlowException("Boom!");
            }
        };

        assertThatThrownBy(() -> codec.decodeMessages(rawMessage)).isInstanceOf(InputProcessingException.class);
    }

    @Test
    public void decodeMessagesThrowsEmptyTemplateExceptionWithIncompleteNetFlowV9() throws Exception {
        final byte[] b = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-3_incomplete.dat"));
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);

        assertThatThrownBy(() -> codec.decodeMessages(new RawMessage(b, source))).isInstanceOf(InputProcessingException.class);

    }

    @Test
    public void aggregatorReleasesBufferedV9PacketWhenTemplateFromEarlierPacketIsKnown() throws Exception {
        final byte[] template256 = new byte[]{
                0x01, 0x00, 0x00, 0x02, // template id 256, 2 fields
                0x00, 0x08, 0x00, 0x04, // ipv4_src_addr
                0x00, 0x0c, 0x00, 0x04 // ipv4_dst_addr
        };
        final byte[] template257 = new byte[]{
                0x01, 0x01, 0x00, 0x02, // template id 257, 2 fields
                0x00, 0x08, 0x00, 0x04, // ipv4_src_addr
                0x00, 0x0c, 0x00, 0x04 // ipv4_dst_addr
        };

        // every datagram comes with its own, equal but not identical, address object
        final CodecAggregator.Result templateResult = codecAggregator.addChunk(
                netFlowV9Packet(flowSet(0, template256)), exporterAddress());
        assertThat(templateResult.getMessage()).isNull();

        // template 257 is still unknown, so the whole packet is buffered
        final CodecAggregator.Result bufferedResult = codecAggregator.addChunk(netFlowV9Packet(
                flowSet(256, new byte[]{10, 0, 0, 1, 10, 0, 0, 100}),
                flowSet(257, new byte[]{10, 0, 0, 2, 10, 0, 0, 100})), exporterAddress());
        assertThat(bufferedResult.isValid()).isTrue();
        assertThat(bufferedResult.getMessage()).isNull();

        final CodecAggregator.Result releasedResult = codecAggregator.addChunk(
                netFlowV9Packet(flowSet(0, template257)), exporterAddress());
        assertThat(releasedResult.isValid()).isTrue();
        assertThat(releasedResult.getMessage()).as("released buffered packet").isNotNull();

        final Collection<Message> messages = codec.decodeMessages(new RawMessage(ByteBufUtil.getBytes(releasedResult.getMessage()), exporterAddress()));
        assertThat(messages)
                .extracting(message -> message.getField("nf_ipv4_src_addr"))
                .containsExactlyInAnyOrder("10.0.0.1", "10.0.0.2");
    }

    private static InetSocketAddress exporterAddress() {
        return new InetSocketAddress("192.168.1.1", 2055);
    }

    private static ByteBuf flowSet(int flowSetId, byte[] content) {
        final ByteBuf flowSet = Unpooled.buffer();
        flowSet.writeShort(flowSetId);
        flowSet.writeShort(4 + content.length);
        flowSet.writeBytes(content);
        return flowSet;
    }

    private static ByteBuf netFlowV9Packet(ByteBuf... flowSets) {
        final ByteBuf packet = Unpooled.buffer();
        packet.writeShort(9); // NetFlow version
        packet.writeShort(flowSets.length); // record count
        packet.writeInt(1000); // system uptime
        packet.writeInt(1700000000); // unix seconds
        packet.writeInt(1); // sequence number
        packet.writeInt(0); // source id
        for (ByteBuf flowSet : flowSets) {
            packet.writeBytes(flowSet);
        }
        return packet;
    }
}
