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

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OutputRouterTest {
    @Mock
    private MessageOutput defaultMessageOutput;
    @Mock
    private OutputRegistry outputRegistry;

    @Test
    public void testAlwaysIncludeDefaultOutput() throws Exception {
        final Message message = mock(Message.class);
        final OutputRouter outputRouter = new OutputRouter(defaultMessageOutput, outputRegistry);

        final Collection<MessageOutput> messageOutputs = outputRouter.getOutputsForMessage(message);

        assertEquals(messageOutputs.size(), 1);
        assertTrue(messageOutputs.contains(defaultMessageOutput));
    }

    @Test
    public void testGetMessageOutputsForEmptyStream() throws Exception {
        final Stream stream = mock(Stream.class);
        final OutputRouter outputRouter = new OutputRouter(defaultMessageOutput, outputRegistry);

        final Collection<MessageOutput> messageOutputs = outputRouter.getMessageOutputsForStream(stream);

        assertEquals(messageOutputs.size(), 0);
    }

    @Test
    public void testGetMessageOutputsForSingleStream() throws Exception {
        final Stream stream = mock(Stream.class);
        final Output output = mock(Output.class);
        final String outputId = "foobar";
        final MessageOutput messageOutput = mock(MessageOutput.class);
        final Set<Output> outputSet = new HashSet<Output>() {{
            add(output);
        }};
        when(stream.getOutputs()).thenReturn(outputSet);
        when(output.getId()).thenReturn(outputId);
        when(outputRegistry.getOutputForIdAndStream(eq(outputId), eq(stream))).thenReturn(messageOutput);
        final OutputRouter outputRouter = new OutputRouter(defaultMessageOutput, outputRegistry);

        final Collection<MessageOutput> messageOutputs = outputRouter.getMessageOutputsForStream(stream);

        assertEquals(messageOutputs.size(), 1);
        assertTrue(messageOutputs.contains(messageOutput));
    }

    @Test
    public void testGetMessageOutputsForStreamWithTwoOutputs() throws Exception {
        final Stream stream = mock(Stream.class);
        final Output output1 = mock(Output.class);
        final Output output2 = mock(Output.class);
        final String output1Id = "foo";
        final String output2Id = "bar";
        final MessageOutput messageOutput1 = mock(MessageOutput.class);
        final MessageOutput messageOutput2 = mock(MessageOutput.class);
        final Set<Output> outputSet = new HashSet<Output>() {{
            add(output1);
            add(output2);
        }};
        when(stream.getOutputs()).thenReturn(outputSet);
        when(output1.getId()).thenReturn(output1Id);
        when(output2.getId()).thenReturn(output2Id);
        when(outputRegistry.getOutputForIdAndStream(eq(output1Id), eq(stream))).thenReturn(messageOutput1);
        when(outputRegistry.getOutputForIdAndStream(eq(output2Id), eq(stream))).thenReturn(messageOutput2);
        final OutputRouter outputRouter = new OutputRouter(defaultMessageOutput, outputRegistry);

        final Collection<MessageOutput> messageOutputs = outputRouter.getMessageOutputsForStream(stream);

        assertEquals(messageOutputs.size(), 2);
        assertTrue(messageOutputs.contains(messageOutput1));
        assertTrue(messageOutputs.contains(messageOutput2));
    }

    @Test
    public void testGetOutputFromSingleStreams() throws Exception {
        final Stream stream = mock(Stream.class);
        final Message message = mock(Message.class);
        when(message.getStreams()).thenReturn(ImmutableSet.of(stream));

        final MessageOutput messageOutput = mock(MessageOutput.class);
        final Set<MessageOutput> messageOutputList = new HashSet<MessageOutput>() {{
            add(messageOutput);
        }};

        final OutputRouter outputRouter = Mockito.spy(new OutputRouter(defaultMessageOutput, outputRegistry));
        doReturn(messageOutputList).when(outputRouter).getMessageOutputsForStream(eq(stream));

        // Call to test
        final Collection<MessageOutput> messageOutputs = outputRouter.getOutputsForMessage(message);

        // Verification
        assertEquals(messageOutputs.size(), 2);
        assertTrue(messageOutputs.contains(defaultMessageOutput));
        assertTrue(messageOutputs.contains(messageOutput));
    }

    @Test
    public void testGetOutputsFromTwoStreams() throws Exception {
        final Stream stream1 = mock(Stream.class);
        final Stream stream2 = mock(Stream.class);
        final MessageOutput messageOutput1 = mock(MessageOutput.class);
        final Set<MessageOutput> messageOutputSet1 = new HashSet<MessageOutput>() {{
            add(messageOutput1);
        }};
        final MessageOutput messageOutput2 = mock(MessageOutput.class);
        final Set<MessageOutput> messageOutputSet2 = new HashSet<MessageOutput>() {{
            add(messageOutput2);
        }};
        final Message message = mock(Message.class);
        when(message.getStreams()).thenReturn(ImmutableSet.of(stream1, stream2));

        OutputRouter outputRouter = Mockito.spy(new OutputRouter(defaultMessageOutput, outputRegistry));
        doReturn(messageOutputSet1).when(outputRouter).getMessageOutputsForStream(eq(stream1));
        doReturn(messageOutputSet2).when(outputRouter).getMessageOutputsForStream(eq(stream2));

        final Collection<MessageOutput> result = outputRouter.getOutputsForMessage(message);

        assertEquals(result.size(), 3);
        assertTrue(result.contains(defaultMessageOutput));
        assertTrue(result.contains(messageOutput1));
        assertTrue(result.contains(messageOutput2));
    }

    @Test
    public void testGetOutputsWithIdenticalMessageOutputs() throws Exception {
        final Stream stream1 = mock(Stream.class);
        final Stream stream2 = mock(Stream.class);
        final MessageOutput messageOutput = mock(MessageOutput.class);
        final Set<MessageOutput> messageOutputSet = new HashSet<MessageOutput>() {{
            add(messageOutput);
        }};
        final Message message = mock(Message.class);
        when(message.getStreams()).thenReturn(ImmutableSet.of(stream1, stream2));

        OutputRouter outputRouter = Mockito.spy(new OutputRouter(defaultMessageOutput, outputRegistry));
        doReturn(messageOutputSet).when(outputRouter).getMessageOutputsForStream(eq(stream1));
        doReturn(messageOutputSet).when(outputRouter).getMessageOutputsForStream(eq(stream2));

        final Collection<MessageOutput> result = outputRouter.getOutputsForMessage(message);

        assertEquals(result.size(), 2);
        assertTrue(result.contains(defaultMessageOutput));
        assertTrue(result.contains(messageOutput));
    }
}
