package org.graylog2.outputs;

import com.google.common.collect.Iterables;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.streams.OutputService;
import org.testng.annotations.Test;

import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class OutputRegistryTest {
    @Test
    public void testMessageOutputsIncludesDefault() {

        MessageOutput messageOutput = mock(MessageOutput.class);
        OutputRegistry registry = new OutputRegistry(messageOutput, null, null);

        Set<MessageOutput> outputs = registry.getMessageOutputs();
        assertSame(Iterables.getOnlyElement(outputs, null), messageOutput, "we should only have the default MessageOutput");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThrowExceptionForUnknownOutputType() throws MessageOutputConfigurationException {
        MessageOutputFactory messageOutputFactory = mock(MessageOutputFactory.class);
        Output output = mock(Output.class);
        when(messageOutputFactory.fromStreamOutput(eq(output))).thenReturn(null);
        OutputRegistry registry = new OutputRegistry(null, null, messageOutputFactory);

        registry.launchOutput(output);

        assertEquals(registry.getRunningMessageOutputs().size(), 0);
    }

    @Test
    public void testLaunchNewOutput() throws Exception {
        final String outputId = "foobar";
        final MessageOutputFactory messageOutputFactory = mock(MessageOutputFactory.class);
        final Output output = mock(Output.class);
        final MessageOutput messageOutput = mock(MessageOutput.class);
        when(messageOutputFactory.fromStreamOutput(eq(output))).thenReturn(messageOutput);
        final OutputService outputService = mock(OutputService.class);
        when(outputService.load(eq(outputId))).thenReturn(output);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, messageOutputFactory);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);

        MessageOutput result = outputRegistry.getOutputForId(outputId);

        assertSame(result, messageOutput);
        assertNotNull(outputRegistry.getRunningMessageOutputs());
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 1);
        verify(result).initialize(any(Configuration.class));
    }

    @Test
    public void testNonExistingInput() throws Exception {
        final String outputId = "foobar";
        final OutputService outputService = mock(OutputService.class);
        when(outputService.load(eq(outputId))).thenThrow(NotFoundException.class);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, null);

        MessageOutput messageOutput = outputRegistry.getOutputForId(outputId);

        assertNull(messageOutput);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);
    }

    @Test()
    public void testInvalidOutputConfiguration() throws Exception {
        final String outputId = "foobar";
        final MessageOutputFactory messageOutputFactory = mock(MessageOutputFactory.class);
        final Output output = mock(Output.class);
        final MessageOutput messageOutput = mock(MessageOutput.class);
        doThrow(new MessageOutputConfigurationException()).when(messageOutput).initialize(any(Configuration.class));
        when(messageOutputFactory.fromStreamOutput(eq(output))).thenReturn(messageOutput);
        final OutputService outputService = mock(OutputService.class);
        when(outputService.load(eq(outputId))).thenReturn(output);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, messageOutputFactory);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);

        MessageOutput result = outputRegistry.getOutputForId(outputId);

        assertNull(result);
        assertEquals(outputRegistry.getRunningMessageOutputs().size(), 0);
    }
}