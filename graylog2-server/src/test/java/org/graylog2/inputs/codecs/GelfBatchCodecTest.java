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
package org.graylog2.inputs.codecs;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.Message;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

public class GelfBatchCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private GelfChunkAggregator aggregator;

    private GelfBatchCodec codec;

    @Before
    public void setUp() {
        codec = new GelfBatchCodec(new Configuration(Collections.emptyMap()), aggregator);
    }

    @Test
    public void decodeMessagesSuccedsWithStream() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"_version\": \"5.11\""
                + "}"
                + "\n"
                + "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"_version\": \"3.11\","
                + "\"foo\": \"bar\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        final ArrayList<Message> messages = (ArrayList<Message>) codec.decodeMessages(rawMessage);

        assertThat(messages).isNotNull();
        assertThat(messages).size().isEqualTo(2);
        assertThat(messages.get(0).getField("version")).isEqualTo("5.11");
        assertThat(messages.get(0).getField("source")).isEqualTo("example.org");
        assertThat(messages.get(1).getField("version")).isEqualTo("3.11");
        assertThat(messages.get(1).getField("foo")).isEqualTo("bar");
    }


    @Test
    public void decodeMessagesSucceedsWithArray() throws Exception {
        final String json = "[{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"_version\": \"5.11\""
                + "}"
                + ","
                + "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"_version\": \"3.11\","
                + "\"foo\": \"bar\""
                + "}]";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        final ArrayList<Message> messages = (ArrayList<Message>) codec.decodeMessages(rawMessage);

        assertThat(messages).isNotNull();
        assertThat(messages).size().isEqualTo(2);
        assertThat(messages.get(0).getField("version")).isEqualTo("5.11");
        assertThat(messages.get(0).getField("source")).isEqualTo("example.org");
        assertThat(messages.get(1).getField("version")).isEqualTo("3.11");
        assertThat(messages.get(1).getField("foo")).isEqualTo("bar");
    }

    @Test
    public void decodeMessagesSucceedsWithMinimalMessages() throws Exception {
        assertThat(codec.decodeMessages(new RawMessage("{\"short_message\":\"0\"}".getBytes(StandardCharsets.UTF_8)))).isNotNull();
        assertThat(codec.decodeMessages(new RawMessage("{\"message\":\"0\"}".getBytes(StandardCharsets.UTF_8)))).isNotNull();
    }

    @Test
    public void decodeMessagesFailsWithEmptyMessage() throws Exception {
        final String json = "[{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": \"\""
                + "}]";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalStateException().isThrownBy(() -> codec.decodeMessages(rawMessage))
                .withNoCause()
                .withMessageMatching("could not find any valid json in the packet");
    }

}
