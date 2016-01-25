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
package org.graylog2.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class ServerProcessBufferProcessorTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private OutputBuffer outputBuffer;
    @Captor
    private ArgumentCaptor<Message> messageArgument;

    @Test
    public void handleMessageRenamesFieldNamesWithDotsIfSanitationIsEnabled() throws Exception {
        final Message message = new Message("Test", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        message.addField("field.string", "Foo");
        message.addField("field.float", 1.0F);
        message.addField("field.double", 1.0D);
        message.addField("field.integer", 1_000);
        message.addField("field.long", 1_000L);
        message.addField("field.boolean", true);
        message.addField("do-not-replace", "Foo");
        final ServerProcessBufferProcessor processor =
                new ServerProcessBufferProcessor(new MetricRegistry(), Collections.emptySet(), outputBuffer, true, "_");

        processor.handleMessage(message);

        verify(outputBuffer).insertBlocking(messageArgument.capture());
        assertThat(messageArgument.getValue().getFieldNames()).containsOnly(
                "_id",
                "message",
                "source",
                "timestamp",
                "field_string",
                "field_float",
                "field_double",
                "field_integer",
                "field_long",
                "field_boolean",
                "do-not-replace"
        );
    }

    @Test
    public void handleMessageDoesNotRenameFieldNamesWithDotsIfSanitationIsDisabled() throws Exception {
        final Message message = new Message("Test", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        message.addField("field.string", "Foo");
        message.addField("field.float", 1.0F);
        message.addField("field.double", 1.0D);
        message.addField("field.integer", 1_000);
        message.addField("field.long", 1_000L);
        message.addField("field.boolean", true);
        final ServerProcessBufferProcessor processor =
                new ServerProcessBufferProcessor(new MetricRegistry(), Collections.emptySet(), outputBuffer, false, "_");

        processor.handleMessage(message);

        verify(outputBuffer).insertBlocking(messageArgument.capture());
        assertThat(messageArgument.getValue().getFieldNames()).containsOnly(
                "_id",
                "message",
                "source",
                "timestamp",
                "field.string",
                "field.float",
                "field.double",
                "field.integer",
                "field.long",
                "field.boolean"
        );
    }

    @Test
    public void handleMessageRunsAllMessageProcessors() {
        final Message message = new Message("Test", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        final Iterable<MessageProcessor> messageProcessors = ImmutableList.of(
                new TestMessageProcessor("foo"), new TestMessageProcessor("bar"));
        final ServerProcessBufferProcessor processor =
                new ServerProcessBufferProcessor(new MetricRegistry(), messageProcessors, outputBuffer, false, "");

        processor.handleMessage(message);

        verify(outputBuffer).insertBlocking(messageArgument.capture());
        assertThat(messageArgument.getValue().getFieldNames()).contains("foo", "bar");
    }

    public static class TestMessageProcessor implements MessageProcessor {
        private final String fieldName;

        public TestMessageProcessor(String fieldName) {
            this.fieldName = requireNonNull(fieldName);
        }

        @Override
        public Messages process(Messages messages) {
            for (Message message : messages) {
                message.addField(fieldName, true);
            }

            return messages;
        }
    }
}