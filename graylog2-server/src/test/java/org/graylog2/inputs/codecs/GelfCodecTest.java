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
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assume.assumeTrue;

public class GelfCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private GelfChunkAggregator aggregator;

    private GelfCodec codec;

    @Before
    public void setUp() {
        codec = new GelfCodec(new Configuration(Collections.emptyMap()), aggregator);
    }

    @Test(expected = IllegalStateException.class)
    public void decodeDoesNotThrowIllegalArgumentExceptionIfJsonIsInvalid() throws Exception {
        // this fails gelf parsing, but empty Payloads are now ok.
        final RawMessage rawMessage = new RawMessage(new byte[0]);
        codec.decode(rawMessage);
    }

    @Test
    public void decodeFiltersOutVersionField() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        final Message message = codec.decode(rawMessage);

        assertThat(message).isNotNull();
        assertThat(message.getField("version")).isNull();
        assertThat(message.getField("source")).isEqualTo("example.org");
    }

    @Test
    public void decodeAllowsSettingCustomVersionField() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"_version\": \"3.11\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        final Message message = codec.decode(rawMessage);

        assertThat(message).isNotNull();
        assertThat(message.getField("version")).isEqualTo("3.11");
        assertThat(message.getField("source")).isEqualTo("example.org");
    }

    @Test
    public void decodeBuildsValidMessageObject() throws Exception {
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
        final Message message = codec.decode(rawMessage);

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
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("JSON is null/could not be parsed (invalid JSON)");
        expectedException.expectCause(isA(JsonParseException.class));

        final Configuration configuration = new Configuration(Collections.singletonMap("decompress_size_limit", 100));
        final GelfCodec codec = new GelfCodec(configuration, aggregator);
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
        codec.decode(rawMessage);
    }

    @Test
    public void getAggregatorReturnsGelfChunkAggregator() throws Exception {
        assertThat(codec.getAggregator()).isSameAs(aggregator);
    }

    @Test
    public void decodeSucceedsWithoutHost() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";
        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        final Message message = codec.decode(rawMessage);
        assertThat(message).isNotNull();
    }

    @Test
    public void decodeFailsWithWrongTypeForHost() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": 42,"
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has invalid \"host\": 42");
    }

    @Test
    public void decodeFailsWithEmptyHost() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"\","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"host\" field.");
    }

    @Test
    public void decodeFailsWithBlankHost() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"      \","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"host\" field.");
    }

    @Test
    public void decodeFailsWithoutShortMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> is missing mandatory \"short_message\" or \"message\" field.");
    }

    @Test
    public void decodeSucceedsWithoutShortMessageButWithMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        final Message message = codec.decode(rawMessage);
        assertThat(message).isNotNull();
    }

    @Test
    public void decodeSucceedsWithEmptyShortMessageButWithMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"\","
                + "\"message\": \"A short message that helps you identify what is going on\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        final Message message = codec.decode(rawMessage);
        assertThat(message).isNotNull();
    }

    @Test
    public void decodeFailsWithWrongTypeForShortMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": 42"
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has invalid \"short_message\": 42");
    }

    @Test
    public void decodeFailsWithWrongTypeForMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": 42"
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has invalid \"message\": 42");
    }

    @Test
    public void decodeFailsWithEmptyShortMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"short_message\" field.");
    }

    @Test
    public void decodeFailsWithEmptyMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": \"\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"message\" field.");
    }

    @Test
    public void decodeFailsWithBlankShortMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"     \""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"short_message\" field.");
    }

    @Test
    public void decodeFailsWithBlankMessage() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"message\": \"     \""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> has empty mandatory \"message\" field.");
    }

    @Test
    public void decodeSucceedsWithWrongTypeForTimestamp() throws Exception {
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
    public void decodeIncludesSourceAddressIfItFails() throws Exception {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\""
                + "}";

        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8), new InetSocketAddress("198.51.100.42", 24783));

        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(rawMessage))
                .withNoCause()
                .withMessageMatching("GELF message <[0-9a-f-]+> \\(received from <198\\.51\\.100\\.42:24783>\\) is missing mandatory \"short_message\" or \"message\" field.");
    }

    @Test
    public void decodeSucceedsWithMinimalMessages() throws Exception {
        assertThat(codec.decode(new RawMessage("{\"short_message\":\"0\"}".getBytes(StandardCharsets.UTF_8)))).isNotNull();
        assertThat(codec.decode(new RawMessage("{\"message\":\"0\"}".getBytes(StandardCharsets.UTF_8)))).isNotNull();
    }

    @Test
    public void decodeSucceedsWithTrailingComma() throws Exception {
        assertThat(codec.decode(new RawMessage("{\"short_message\":\"0\",}".getBytes(StandardCharsets.UTF_8)))).isNotNull();
        assertThat(codec.decode(new RawMessage("{\"message\":\"0\",}".getBytes(StandardCharsets.UTF_8)))).isNotNull();
    }

    @Test
    public void decodeSucceedsWithValidTimestampIssue4027() throws Exception {
        // https://github.com/Graylog2/graylog2-server/issues/4027
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"host\": \"example.org\","
                + "\"timestamp\": 1500646980.661"
                + "}";
        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        final Message message = codec.decode(rawMessage);
        assertThat(message).isNotNull();
        assertThat(message.getTimestamp()).isEqualTo(DateTime.parse("2017-07-21T14:23:00.661Z"));
    }

    @Test
    public void decodeSucceedsWithValidTimestampAsStringIssue4027() throws Exception {
        // https://github.com/Graylog2/graylog2-server/issues/4027
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"short_message\": \"A short message that helps you identify what is going on\","
                + "\"host\": \"example.org\","
                + "\"timestamp\": \"1500646980.661\""
                + "}";
        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));

        final Message message = codec.decode(rawMessage);
        assertThat(message).isNotNull();
        assertThat(message.getTimestamp()).isEqualTo(DateTime.parse("2017-07-21T14:23:00.661Z"));
    }
}
