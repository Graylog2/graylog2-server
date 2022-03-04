/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.outputs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import org.graylog2.database.NotFoundException;
import org.graylog2.outputs.events.OutputChangedEvent;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutputRegistryTest {
    private static final long FAULT_COUNT_THRESHOLD = 5;
    private static final long FAULT_PENALTY_SECONDS = 30;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MessageOutput messageOutput;
    @Mock
    private MessageOutputFactory messageOutputFactory;
    @Mock
    private Output output;
    @Mock
    private OutputService outputService;
    @Mock
    private EventBus eventBus;
    @Mock
    private StreamService streamService;

    private OutputRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new OutputRegistry(messageOutput, outputService, messageOutputFactory, null,
                null, eventBus, streamService, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);
    }

    @Test
    public void testMessageOutputsIncludesDefault() {
        Set<MessageOutput> outputs = registry.getMessageOutputs();
        assertSame("we should only have the default MessageOutput", Iterables.getOnlyElement(outputs, null), messageOutput);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowExceptionForUnknownOutputType() throws MessageOutputConfigurationException {
        registry.launchOutput(output, null);
    }

    @Test
    public void testLaunchNewOutput() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(messageOutputFactory.fromStreamOutput(eq(output), eq(stream), any(Configuration.class))).thenReturn(messageOutput);
        when(outputService.load(eq(outputId))).thenReturn(output);

        assertEquals(0, registry.getRunningMessageOutputs().size());

        MessageOutput result = registry.getOutputForIdAndStream(outputId, stream);

        assertSame(result, messageOutput);
        assertNotNull(registry.getRunningMessageOutputs());
        assertEquals(1, registry.getRunningMessageOutputs().size());
    }

    @Test
    public void testNonExistingInput() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(outputService.load(eq(outputId))).thenThrow(NotFoundException.class);

        MessageOutput messageOutput = registry.getOutputForIdAndStream(outputId, stream);

        assertNull(messageOutput);
        assertEquals(0, registry.getRunningMessageOutputs().size());
    }

    @Test
    public void testInvalidOutputConfiguration() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(messageOutputFactory.fromStreamOutput(eq(output), any(Stream.class), any(Configuration.class))).thenThrow(new MessageOutputConfigurationException());
        when(outputService.load(eq(outputId))).thenReturn(output);

        assertEquals(0, registry.getRunningMessageOutputs().size());

        MessageOutput result = registry.getOutputForIdAndStream(outputId, stream);

        assertNull(result);
        assertEquals(0, registry.getRunningMessageOutputs().size());
    }

    @Test
    public void testHandlesOutputChanged() throws Exception {
        loadIntoRegistry(output("output-1"), output("output-2"));
        assertThat(registry.getRunningMessageOutputs()).containsOnlyKeys("output-1", "output-2");

        registry.handleOutputChanged(OutputChangedEvent.create("output-1"));

        assertThat(registry.getRunningMessageOutputs()).containsOnlyKeys("output-2");
    }

    @Test
    public void testHandlesOutputDeleted() throws Exception {
        loadIntoRegistry(output("output-1"), output("output-2"));
        assertThat(registry.getRunningMessageOutputs()).containsOnlyKeys("output-1", "output-2");

        registry.handleOutputChanged(OutputChangedEvent.create("output-2"));

        assertThat(registry.getRunningMessageOutputs()).containsOnlyKeys("output-1");
    }

    @Test
    public void testHandlesStreamsChanged() throws Exception {
        Output output1 = output("output-1");
        Output output2 = output("output-2");
        Output output3 = output("output-3");
        Output output4 = output("output-4");

        loadIntoRegistry(output1, output2, output3, output4);
        assertThat(registry.getRunningMessageOutputs()).containsOnlyKeys("output-1", "output-2", "output-3", "output-4");

        when(streamService.loadAllEnabled()).thenReturn(ImmutableList.of(
                stream(output1, output2), stream(output3)));

        registry.handleStreamsChanged(StreamsChangedEvent.create("ignored-stream-id"));

        assertThat(registry.getRunningMessageOutputs()).containsOnlyKeys("output-1", "output-2", "output-3");
    }

    private void loadIntoRegistry(Output... outputs) throws Exception {
        for (final Output output : outputs) {
            Stream stream = mock(Stream.class);
            when(outputService.load(eq(output.getId()))).thenReturn(output);
            when(messageOutputFactory.fromStreamOutput(eq(output), eq(stream), any(Configuration.class)))
                    .thenReturn(messageOutput);
            registry.getOutputForIdAndStream(output.getId(), stream);
        }
    }

    private Output output(String outputId) {
        return OutputImpl.create(outputId, "", "", "", Collections.emptyMap(), new Date(), null);
    }

    private Stream stream(Output... outputs) {
        return new StreamImpl(null, null, null, ImmutableSet.copyOf(outputs), null);
    }
}
