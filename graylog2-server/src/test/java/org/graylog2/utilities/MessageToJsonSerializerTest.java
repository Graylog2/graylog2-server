/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Lists;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class MessageToJsonSerializerTest {
    @Mock private ObjectMapper objectMapper;
    @Mock private StreamService streamService;
    @Mock private InputService inputService;
    @Mock private MessageInput messageInput;
    @Mock private Input input;
    @Mock private Stream stream;

    @BeforeMethod
    public void setUp() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());

        MockitoAnnotations.initMocks(this);

        when(objectMapper.copy()).thenReturn(mapper);
        when(stream.getId()).thenReturn("stream-id");
        when(messageInput.getId()).thenReturn("input-id");
        when(inputService.buildMessageInput(input)).thenReturn(messageInput);
        when(inputService.find("input-id")).thenReturn(input);
        when(streamService.load("stream-id")).thenReturn(stream);
    }

    @Test
    public void shouldSerializeMessageCorrectly() throws Exception {
        final MessageToJsonSerializer serializer = new MessageToJsonSerializer(objectMapper, streamService, inputService);
        final DateTime now = Tools.iso8601();
        final Message message = new Message("test", "localhost", now);

        message.setSourceInput(messageInput);
        message.setStreams(Lists.newArrayList(stream));
        message.addField("test1", "hello");
        message.addField("test2", 1);
        message.addField("test3", 1.2);
        message.addField("test4", false);

        final String s = serializer.serializeToString(message);

        final Message newMessage = serializer.deserialize(s);

        assertEquals(newMessage.getField("timestamp"), now);
        assertEquals(newMessage.getMessage(), message.getMessage());
        assertEquals(newMessage.getSource(), message.getSource());
        assertEquals(newMessage.getSourceInput(), messageInput);
        assertEquals(newMessage.getStreams(), Lists.newArrayList(stream));
        assertEquals(newMessage.getField("test1"), "hello");
        assertEquals(newMessage.getField("test2"), 1);
        assertEquals(newMessage.getField("test3"), 1.2);
        assertEquals(newMessage.getField("test4"), false);

        // Just assert that the message id is not null because we cannot set the _id field on deserialize because the
        // Message object does not allow the _id field to be set.
        assertNotNull(newMessage.getId());

        // Make sure the injected ObjectMapper instance is copied before adding custom config.
        verify(objectMapper, times(1)).copy();
    }
}