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
package org.graylog2.messageprocessors;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamMock;
import org.graylog2.streams.StreamRouter;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StreamMatcherProcessorTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StreamRouter streamRouter;
    private StreamMatcherProcessor processor;

    @Before
    public void setUp() throws Exception {
        processor = new StreamMatcherProcessor(streamRouter);
    }

    @Test
    public void process_adds_streams_to_messages() throws Exception {
        final Message message = new Message("message", "source", DateTime.parse("2017-09-27T16:00:00Z"));
        final TestMessages messages = new TestMessages(Collections.singleton(message));
        final Stream stream = new StreamMock(
                ImmutableMap.of(
                        "_id", "stream-id",
                        "title", "title",
                        "description", "description"
                ));
        when(streamRouter.route(message)).thenReturn(Collections.singletonList(stream));

        assertThat(message.getStreams()).isEmpty();

        final Messages processedMessages = processor.process(messages);

        assertThat(processedMessages).hasSameSizeAs(messages);

        final Message processedMessage = processedMessages.iterator().next();
        assertThat(processedMessage.getFields()).isEqualTo(message.getFields());
        assertThat(processedMessage.getStreams()).containsExactly(stream);
    }

    @Test
    public void process_does_not_add_streams_to_messages_if_none_match() throws Exception {
        final TestMessages messages = new TestMessages(Arrays.asList(
                new Message("message1", "source1", DateTime.parse("2017-09-27T16:00:00Z")),
                new Message("message2", "source2", DateTime.parse("2017-09-27T16:00:01Z"))
        ));

        when(streamRouter.route(any(Message.class))).thenReturn(Collections.emptyList());

        final Messages processedMessages = processor.process(messages);

        assertThat(processedMessages)
                .hasSameSizeAs(messages)
                .isEqualTo(messages);
    }

    private static class TestMessages extends ArrayList<Message> implements Messages {
        private TestMessages(Collection<? extends Message> c) {
            super(c);
        }
    }
}