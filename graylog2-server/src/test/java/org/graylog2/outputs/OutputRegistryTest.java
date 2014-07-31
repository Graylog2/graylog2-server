package org.graylog2.outputs;

import com.google.common.collect.Iterables;
import org.graylog2.plugin.outputs.MessageOutput;
import org.testng.annotations.Test;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertSame;

public class OutputRegistryTest {

    @Test
    public void testMessageOutputsIncludesDefault() {

        MessageOutput messageOutput = mock(MessageOutput.class);
        OutputRegistry registry = new OutputRegistry(messageOutput, null, null);

        Set<MessageOutput> outputs = registry.getMessageOutputs();
        assertSame(Iterables.getOnlyElement(outputs, null), messageOutput, "we should only have the default MessageOutput");
    }

}