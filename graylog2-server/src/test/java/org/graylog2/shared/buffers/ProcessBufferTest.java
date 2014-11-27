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
package org.graylog2.shared.buffers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.lmax.disruptor.BlockingWaitStrategy;
import org.graylog2.inputs.InputCache;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Test
public class ProcessBufferTest {
    private MetricRegistry metricRegistry;
    private ServerStatus serverStatus;

    private MetricRegistry getMockMetricRegistry() {
        final MetricRegistry mockMetricRegistry = mock(MetricRegistry.class);
        final Meter mockMeter = mock(Meter.class);
        final Timer mockTimer = mock(Timer.class);
        final Counter mockCounter = mock(Counter.class);
        when(mockMetricRegistry.meter(anyString())).thenReturn(mockMeter);
        when(mockMetricRegistry.timer(anyString())).thenReturn(mockTimer);
        when(mockMetricRegistry.counter(anyString())).thenReturn(mockCounter);

        return mockMetricRegistry;
    }

    @BeforeMethod
    public void setUp() throws Exception {
        metricRegistry = getMockMetricRegistry();
        serverStatus = mock(ServerStatus.class);
        when(serverStatus.isProcessing()).thenReturn(true);
    }

    public void testBasicInsert() throws Exception {
        ProcessBuffer processBuffer = new ProcessBuffer(metricRegistry, serverStatus, mock(BaseConfiguration.class), mock(InputCache.class));

        ProcessBufferProcessor processBufferProcessor = mock(ProcessBufferProcessor.class);
        ProcessBufferProcessor[] processBufferProcessors = new ProcessBufferProcessor[1];
        processBufferProcessors[0] = processBufferProcessor;

        processBuffer.initialize(processBufferProcessors, 1, new BlockingWaitStrategy(), 1);

        Message message = mock(Message.class);
        MessageInput messageInput = mock(MessageInput.class);

        processBuffer.insertFailFast(message, messageInput);
    }
}
