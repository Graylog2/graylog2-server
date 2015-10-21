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
package org.graylog2.buffers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.buffers.processors.DecodingProcessor;
import org.graylog2.buffers.processors.ProcessBufferProcessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Provider;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void setUp() throws Exception {
        metricRegistry = getMockMetricRegistry();
        serverStatus = mock(ServerStatus.class);
        when(serverStatus.isProcessing()).thenReturn(true);
    }

    @Test
    public void testBasicInsert() throws Exception {
        final Provider provider = mock(Provider.class);
        when(provider.get()).thenReturn(mock(ProcessBufferProcessor.class));
        ProcessBuffer processBuffer = new ProcessBuffer(metricRegistry, serverStatus, mock(DecodingProcessor.Factory.class),
                provider, 1, 1, "blocking");


        RawMessage message = mock(RawMessage.class);
        MessageInput messageInput = mock(MessageInput.class);

        processBuffer.insertBlocking(message);
    }
}
