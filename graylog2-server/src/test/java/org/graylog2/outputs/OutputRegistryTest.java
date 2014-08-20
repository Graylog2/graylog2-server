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
package org.graylog2.outputs;

import com.google.common.collect.Iterables;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.streams.OutputService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Test
public class OutputRegistryTest {
    @Mock
    private MessageOutput messageOutput;
    @Mock
    private MessageOutputFactory messageOutputFactory;
    @Mock
    private Output output;
    @Mock
    private OutputService outputService;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testMessageOutputsIncludesDefault() {
        OutputRegistry registry = new OutputRegistry(messageOutput, null, null);

        Set<MessageOutput> outputs = registry.getMessageOutputs();
        assertSame(Iterables.getOnlyElement(outputs, null), messageOutput, "we should only have the default MessageOutput");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThrowExceptionForUnknownOutputType() throws MessageOutputConfigurationException {
        when(messageOutputFactory.fromStreamOutput(eq(output))).thenReturn(null);
        OutputRegistry registry = new OutputRegistry(null, null, messageOutputFactory);

        registry.launchOutput(output);

        assertEquals(registry.getRunningMessageOutputs().size(), 0);
    }

    public void testLaunchNewOutput() throws Exception {
        final String outputId = "foobar";
        when(messageOutputFactory.fromStreamOutput(eq(output))).thenReturn(messageOutput);
        when(outputService.load(eq(outputId))).thenReturn(output);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, messageOutputFactory);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);

        MessageOutput result = outputRegistry.getOutputForId(outputId);

        assertSame(result, messageOutput);
        assertNotNull(outputRegistry.getRunningMessageOutputs());
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 1);
        verify(result).initialize(any(Configuration.class));
    }

    public void testNonExistingInput() throws Exception {
        final String outputId = "foobar";
        when(outputService.load(eq(outputId))).thenThrow(NotFoundException.class);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, null);

        MessageOutput messageOutput = outputRegistry.getOutputForId(outputId);

        assertNull(messageOutput);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);
    }

    public void testInvalidOutputConfiguration() throws Exception {
        final String outputId = "foobar";
        doThrow(new MessageOutputConfigurationException()).when(messageOutput).initialize(any(Configuration.class));
        when(messageOutputFactory.fromStreamOutput(eq(output))).thenReturn(messageOutput);
        when(outputService.load(eq(outputId))).thenReturn(output);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, messageOutputFactory);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);

        MessageOutput result = outputRegistry.getOutputForId(outputId);

        assertNull(result);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);
    }
}