/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

package org.graylog2.inputs.gelf.gelf;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Test
public class GELFProcessorTest {
    @Mock GELFMessage gelfMessage;
    @Mock MessageInput messageInput;
    @Mock Buffer processBuffer;
    @Mock MetricRegistry metricRegistry;
    @Mock GELFParser gelfParser;
    @Mock Message message;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Meter meter = mock(Meter.class);
        when(metricRegistry.meter(anyString())).thenReturn(meter);
        Timer timer = mock(Timer.class);
        when(metricRegistry.timer(anyString())).thenReturn(timer);

        when(gelfMessage.getJSON()).thenReturn("");

        when(gelfParser.parse(anyString(), eq(messageInput))).thenReturn(message);

        when(message.isComplete()).thenReturn(true);
    }

    public void testMessageReceived() throws Exception {
        GELFProcessor gelfProcessor = new GELFProcessor(metricRegistry, processBuffer, gelfParser);

        gelfProcessor.messageReceived(gelfMessage, messageInput);

        verify(processBuffer).insertCached(eq(message), eq(messageInput));
    }

    public void testMessageReceivedFailFast() throws Exception {
        GELFProcessor gelfProcessor = new GELFProcessor(metricRegistry, processBuffer, gelfParser);

        gelfProcessor.messageReceivedFailFast(gelfMessage, messageInput);

        verify(processBuffer).insertFailFast(eq(message), eq(messageInput));
    }

    public void testMessageReceivedIncompleteMessage() throws Exception {
        GELFProcessor gelfProcessor = new GELFProcessor(metricRegistry, processBuffer, gelfParser);
        when(message.isComplete()).thenReturn(false);

        gelfProcessor.messageReceived(gelfMessage, messageInput);

        verify(processBuffer, never()).insertCached(any(Message.class), any(MessageInput.class));
    }

    public void testMessageReceivedFailFastIncompleteMessage() throws Exception {
        GELFProcessor gelfProcessor = new GELFProcessor(metricRegistry, processBuffer, gelfParser);
        when(message.isComplete()).thenReturn(false);

        gelfProcessor.messageReceivedFailFast(gelfMessage, messageInput);

        verify(processBuffer, never()).insertFailFast(any(Message.class), any(MessageInput.class));
    }

    public void testMessageReceivedCorruptMessage() throws Exception {
        GELFProcessor gelfProcessor = new GELFProcessor(metricRegistry, processBuffer, gelfParser);
        when(gelfParser.parse(anyString(), eq(messageInput))).thenThrow(new IllegalStateException());

        gelfProcessor.messageReceived(gelfMessage, messageInput);

        verify(processBuffer, never()).insertCached(any(Message.class), any(MessageInput.class));
    }

    public void testMessageReceivedFailFastCorruptMessage() throws Exception {
        GELFProcessor gelfProcessor = new GELFProcessor(metricRegistry, processBuffer, gelfParser);
        when(gelfParser.parse(anyString(), eq(messageInput))).thenThrow(new IllegalStateException());

        gelfProcessor.messageReceivedFailFast(gelfMessage, messageInput);

        verify(processBuffer, never()).insertFailFast(any(Message.class), any(MessageInput.class));
    }
}
