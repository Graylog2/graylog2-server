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
import org.graylog.plugins.netflow.flows.FlowException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

public class NetFlowCodecTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NetFlowCodec codec;
    private NetflowV9CodecAggregator codecAggregator;

    @Before
    public void setUp() throws Exception {
        codecAggregator = new NetflowV9CodecAggregator();
        codec = new NetFlowCodec(Configuration.EMPTY_CONFIGURATION, codecAggregator);
    }

    @Test
    public void constructorFailsIfNetFlow9DefinitionsPathDoesNotExist() throws Exception {
        final File definitionsFile = temporaryFolder.newFile();
        assertThat(definitionsFile.delete()).isTrue();

        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                NetFlowCodec.CK_NETFLOW9_DEFINITION_PATH, definitionsFile.getAbsolutePath());
        final Configuration configuration = new Configuration(configMap);

        assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> new NetFlowCodec(configuration, codecAggregator))
                .withMessageEndingWith("(No such file or directory)");
    }

    @Test
    public void constructorSucceedsIfNetFlow9DefinitionsPathIsEmpty() throws Exception {
        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                NetFlowCodec.CK_NETFLOW9_DEFINITION_PATH, "");
        final Configuration configuration = new Configuration(configMap);

        assertThat(new NetFlowCodec(configuration, codecAggregator)).isNotNull();
    }

    @Test
    public void constructorSucceedsIfNetFlow9DefinitionsPathIsBlank() throws Exception {
        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                NetFlowCodec.CK_NETFLOW9_DEFINITION_PATH, "   ");
        final Configuration configuration = new Configuration(configMap);

        assertThat(new NetFlowCodec(configuration, codecAggregator)).isNotNull();
    }

    @Test
    public void constructorFailsIfNetFlow9DefinitionsPathIsInvalidYaml() throws Exception {
        final File definitionsFile = temporaryFolder.newFile();
        Files.write(definitionsFile.toPath(), "foo: %bar".getBytes(StandardCharsets.UTF_8));

        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                NetFlowCodec.CK_NETFLOW9_DEFINITION_PATH, definitionsFile.getAbsolutePath());
        final Configuration configuration = new Configuration(configMap);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NetFlowCodec(configuration, codecAggregator))
                .withMessageMatching("Unable to parse NetFlow 9 definitions");
    }

    @Test
    public void decodeThrowsUnsupportedOperationException() throws Exception {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> codec.decode(new RawMessage(new byte[0])))
                .withMessage("MultiMessageCodec " + NetFlowCodec.class + " does not support decode()");
    }

    @Test
    public void decodeMessagesReturnsNullIfMessageWasInvalid() throws Exception {
        final byte[] b = "Foobar".getBytes(StandardCharsets.UTF_8);
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);
        final RawMessage rawMessage = new RawMessage(b, source);

        final Collection<Message> messages = codec.decodeMessages(rawMessage);
        assertThat(messages).isNull();
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

        final Collection<Message> messages = codec.decodeMessages(rawMessage);
        assertThat(messages).isNull();
    }

    @Test
    public void decodeMessagesThrowsEmptyTemplateExceptionWithIncompleteNetFlowV9() throws Exception {
        final byte[] b = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-3_incomplete.dat"));
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);

        assertThat(codec.decodeMessages(new RawMessage(b, source))).isNull();
    }
}