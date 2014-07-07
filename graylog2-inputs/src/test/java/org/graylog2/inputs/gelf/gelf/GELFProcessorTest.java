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

        verify(processBuffer).insertCached(any(Message.class), eq(messageInput));
    }

    public void testMessageReceivedFailFast() throws Exception {
        GELFProcessor gelfProcessor = new GELFProcessor(metricRegistry, processBuffer, gelfParser);

        gelfProcessor.messageReceivedFailFast(gelfMessage, messageInput);

        verify(processBuffer).insertFailFast(any(Message.class), eq(messageInput));
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
