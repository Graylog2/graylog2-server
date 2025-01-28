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

import com.fasterxml.jackson.core.JsonParseException;
import org.graylog2.inputs.TestHelper;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;

@ExtendWith(MockitoExtension.class)
public class GelfCodecTest {

    @Mock
    private GelfChunkAggregator aggregator;

    private GelfCodec codec;

    private final MessageFactory messageFactory = new TestMessageFactory();

    @Before
    public void setUp() {
        codec = new GelfCodec(new Configuration(Collections.emptyMap()), aggregator, messageFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void decodeDoesNotThrowIllegalArgumentExceptionIfJsonIsInvalid() {
        // this fails gelf parsing, but empty Payloads are now ok.
        final RawMessage rawMessage = new RawMessage(new byte[0]);
        codec.decodeSafe(rawMessage);
    }

    @Test
    public void decodeFiltersOutVersionField() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        final Message message = codec.decodeSafe(rawMessage).get();

        assertThat(message).isNotNull();
        assertThat(message.getField("version")).isNull();
        assertThat(message.getField("source")).isEqualTo("example.org");
    }

    @Test
    public void decodeAllowsSettingCustomVersionField() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"_version\": \"3.11\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        final Message message = codec.decodeSafe(rawMessage).get();

        assertThat(message).isNotNull();
        assertThat(message.getField("version")).isEqualTo("3.11");
        assertThat(message.getField("source")).isEqualTo("example.org");
    }

    @Test
    public void decodeBuildsValidMessageObject() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"full_message\": \"Backtrace here\\n\\nMore stuff\","
                + "\"timestamp\": 1385053862.3072,"
                + "\"level\": 1,"
                + "\"_user_id\": 9001,"
                + "\"_some_info\": \"foo\","
                + "\"_some_env_var\": \"bar\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        final Message message = codec.decodeSafe(rawMessage).get();

        assertThat(message).isNotNull();
        assertThat(message.getField("source")).isEqualTo("example.org");
        assertThat(message.getField("message")).isEqualTo("A short message that helps you identify what is going on");
        assertThat(message.getField("user_id")).isEqualTo(9001L);
        assertThat(message.getFieldNames()).containsOnly(
                "_id", "source", "message", "full_message", "timestamp", "level",
                "user_id", "some_info", "some_env_var");
    }

    @Test
    public void decodeLargeCompressedMessageFails() throws Exception {
        final Configuration configuration = new Configuration(Collections.singletonMap("decompress_size_limit", 100));
        final GelfCodec codec = new GelfCodec(configuration, aggregator, messageFactory);
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"full_message\": \"Backtrace here\\n\\nMore stuff\","
                + "\"timestamp\": 1385053862.3072,"
                + "\"level\": 1,"
                + "\"_some_bytes1\": \"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, \","
                + "\"_some_bytes2\": \"sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, \","
                + "\"_some_bytes2\": \"sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum.\""
                + "}";

        final byte[] payload = TestHelper.zlibCompress(json);
        assumeTrue(payload.length > 100);
        final RawMessage rawMessage = new RawMessage(payload);
        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasCauseInstanceOf(JsonParseException.class)
                .hasMessage("JSON is null/could not be parsed (invalid JSON)");
    }

    @Test
    public void getAggregatorReturnsGelfChunkAggregator() {
        assertThat(codec.getAggregator()).isSameAs(aggregator);
    }

    @Test
    public void decodeSucceedsWithoutHost() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";
        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        final Message message = codec.decodeSafe(rawMessage).get();
        assertThat(message).isNotNull();
    }

    @Test
    public void decodeFailsWithWrongTypeForHost() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": 42,"
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has invalid \"host\": 42");
    }

    @Test
    public void decodeFailsWithEmptyHost() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"\","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"host\" field.");
    }

    @Test
    public void decodeFailsWithBlankHost() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"      \","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"host\" field.");
    }

    @Test
    public void decodeFailsWithoutShortMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> is missing mandatory \"short_message\" or \"message\" field.");
    }

    @Test
    public void decodeSucceedsWithoutShortMessageButWithMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThat(codec.decodeSafe(rawMessage)).isNotEmpty();
    }

    @Test
    public void decodeSucceedsWithEmptyShortMessageButWithMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"\","
                + "\"message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThat(codec.decodeSafe(rawMessage)).isNotEmpty();
    }

    @Test
    public void decodeFailsWithWrongTypeForShortMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": 42"
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has invalid \"short_message\": 42");
    }

    @Test
    public void decodeFailsWithWrongTypeForMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": 42"
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has invalid \"message\": 42");
    }

    @Test
    public void decodeFailsWithEmptyShortMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"short_message\" field.");
    }

    @Test
    public void decodeFailsWithEmptyMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": \"\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"message\" field.");
    }

    @Test
    public void decodeFailsWithBlankShortMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"     \""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"short_message\" field.");
    }

    @Test
    public void decodeFailsWithBlankMessage() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": \"     \""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"message\" field.");
    }

    @Test
    public void decodeSucceedsWithWrongTypeForTimestamp() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"timestamp\": \"Foobar\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThat(rawMessage).isNotNull();
    }

    @Test
    public void decodeIncludesSourceAddressIfItFails() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8), new InetSocketAddress("198.51.100.42", 24783));

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageMatching("GELF message <[0-9a-f-]+> \\(received from <198\\.51\\.100\\.42:24783>\\) is missing mandatory \"short_message\" or \"message\" field.");
    }

    @Test
    public void decodeSucceedsWithMinimalMessages() {
        assertThat(codec.decodeSafe(new RawMessage("{\"short_message\":\"0\"}".getBytes(StandardCharsets.UTF_8)))).isNotEmpty();
        assertThat(codec.decodeSafe(new RawMessage("{\"message\":\"0\"}".getBytes(StandardCharsets.UTF_8)))).isNotEmpty();
    }

    @Test
    public void decodeSucceedsWithTrailingComma() {
        assertThat(codec.decodeSafe(new RawMessage("{\"short_message\":\"0\",}".getBytes(StandardCharsets.UTF_8)))).isNotEmpty();
        assertThat(codec.decodeSafe(new RawMessage("{\"message\":\"0\",}".getBytes(StandardCharsets.UTF_8)))).isNotEmpty();
    }

    @Test
    public void decodeSucceedsWithValidTimestampIssue4027() {
        // https://github.com/Graylog2/graylog2-server/issues/4027
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"host\": \"example.org\","
                + "\"timestamp\": 1500646980.661"
                + "}";
        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        final Message message = codec.decodeSafe(rawMessage).get();
        assertThat(message).isNotNull();
        assertThat(message.getTimestamp()).isEqualTo(DateTime.parse("2017-07-21T14:23:00.661Z"));
    }

    @Test
    public void decodeSucceedsWithValidTimestampAsStringIssue4027() {
        // https://github.com/Graylog2/graylog2-server/issues/4027
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"host\": \"example.org\","
                + "\"timestamp\": \"1500646980.661\""
                + "}";
        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        final Message message = codec.decodeSafe(rawMessage).get();
        assertThat(message).isNotNull();
        assertThat(message.getTimestamp()).isEqualTo(DateTime.parse("2017-07-21T14:23:00.661Z"));
    }
}
