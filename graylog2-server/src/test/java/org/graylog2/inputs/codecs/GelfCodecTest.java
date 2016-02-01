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
package org.graylog2.inputs.codecs;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class GelfCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private GelfChunkAggregator aggregator;

    private GelfCodec codec;

    @Before
    public void setUp() {
        codec = new GelfCodec(new Configuration(Collections.emptyMap()), aggregator);
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeThrowsIllegalArgumentExceptionIfJsonIsInvalid() throws Exception {
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
    public void getAggregatorReturnsGelfChunkAggregator() throws Exception {
        assertThat(codec.getAggregator()).isSameAs(aggregator);
    }
}