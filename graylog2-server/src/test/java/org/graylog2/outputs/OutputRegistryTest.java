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
package org.graylog2.outputs;

import com.google.common.collect.Iterables;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.OutputService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OutputRegistryTest {
    private static final long FAULT_COUNT_THRESHOLD = 5;
    private static final long FAULT_PENALTY_SECONDS = 30;

    @Mock
    private MessageOutput messageOutput;
    @Mock
    private MessageOutputFactory messageOutputFactory;
    @Mock
    private Output output;
    @Mock
    private OutputService outputService;
    @Mock
    private org.graylog2.Configuration configuration;

    @Before
    public void setUp() throws Exception {
        when(configuration.getOutputFaultCountThreshold()).thenReturn(FAULT_COUNT_THRESHOLD);
        when(configuration.getOutputFaultPenaltySeconds()).thenReturn(FAULT_PENALTY_SECONDS);
    }

    @Test
    public void testMessageOutputsIncludesDefault() {
        OutputRegistry registry = new OutputRegistry(messageOutput, null, null, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);

        Set<MessageOutput> outputs = registry.getMessageOutputs();
        assertSame("we should only have the default MessageOutput", Iterables.getOnlyElement(outputs, null), messageOutput);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowExceptionForUnknownOutputType() throws MessageOutputConfigurationException {
        when(messageOutputFactory.fromStreamOutput(eq(output), any(Stream.class), any(Configuration.class))).thenReturn(null);
        OutputRegistry registry = new OutputRegistry(null, null, messageOutputFactory, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);

        registry.launchOutput(output, null);

        assertEquals(registry.getRunningMessageOutputs().size(), 0);
    }

    @Test
    public void testLaunchNewOutput() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(messageOutputFactory.fromStreamOutput(eq(output), eq(stream), any(Configuration.class))).thenReturn(messageOutput);
        when(outputService.load(eq(outputId))).thenReturn(output);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, messageOutputFactory, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);

        MessageOutput result = outputRegistry.getOutputForIdAndStream(outputId, stream);

        assertSame(result, messageOutput);
        assertNotNull(outputRegistry.getRunningMessageOutputs());
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 1);
    }

    @Test
    public void testNonExistingInput() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(outputService.load(eq(outputId))).thenThrow(NotFoundException.class);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, null, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);

        MessageOutput messageOutput = outputRegistry.getOutputForIdAndStream(outputId, stream);

        assertNull(messageOutput);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);
    }

    @Test
    public void testInvalidOutputConfiguration() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(messageOutputFactory.fromStreamOutput(eq(output), any(Stream.class), any(Configuration.class))).thenThrow(new MessageOutputConfigurationException());
        when(outputService.load(eq(outputId))).thenReturn(output);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, messageOutputFactory, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);

        MessageOutput result = outputRegistry.getOutputForIdAndStream(outputId, stream);

        assertNull(result);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);
    }
}
