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

import com.google.common.collect.Iterables;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.OutputService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Set;

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

    @Test
    public void testMessageOutputsIncludesDefault() {
        OutputRegistry registry = new OutputRegistry(messageOutput, null, null, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);

        Set<MessageOutput> outputs = registry.getMessageOutputs();
        assertSame("we should only have the default MessageOutput", Iterables.getOnlyElement(outputs, null), messageOutput);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowExceptionForUnknownOutputType() throws MessageOutputConfigurationException {
        OutputRegistry registry = new OutputRegistry(null, null, messageOutputFactory, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);

        registry.launchOutput(output, null);
    }

    @Test
    public void testLaunchNewOutput() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(messageOutputFactory.fromStreamOutput(eq(output), eq(stream), any(Configuration.class))).thenReturn(messageOutput);
        when(outputService.load(eq(outputId))).thenReturn(output);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, messageOutputFactory, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);
        assertEquals(0, outputRegistry.getRunningMessageOutputs().size());

        MessageOutput result = outputRegistry.getOutputForIdAndStream(outputId, stream);

        assertSame(result, messageOutput);
        assertNotNull(outputRegistry.getRunningMessageOutputs());
        assertEquals(1, outputRegistry.getRunningMessageOutputs().size());
    }

    @Test
    public void testNonExistingInput() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(outputService.load(eq(outputId))).thenThrow(NotFoundException.class);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, null, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);

        MessageOutput messageOutput = outputRegistry.getOutputForIdAndStream(outputId, stream);

        assertNull(messageOutput);
        assertEquals(0, outputRegistry.getRunningMessageOutputs().size());
    }

    @Test
    public void testInvalidOutputConfiguration() throws Exception {
        final String outputId = "foobar";
        final Stream stream = mock(Stream.class);
        when(messageOutputFactory.fromStreamOutput(eq(output), any(Stream.class), any(Configuration.class))).thenThrow(new MessageOutputConfigurationException());
        when(outputService.load(eq(outputId))).thenReturn(output);

        final OutputRegistry outputRegistry = new OutputRegistry(null, outputService, messageOutputFactory, null, null, FAULT_COUNT_THRESHOLD, FAULT_PENALTY_SECONDS);
        assertEquals(0, outputRegistry.getRunningMessageOutputs().size());

        // temporarily set logger-level to off to suppress deceptive exception-stacktrace
        Configurator.setLevel(OutputRegistry.class.getName(), Level.OFF);
        MessageOutput result = outputRegistry.getOutputForIdAndStream(outputId, stream);
        Configurator.setLevel(OutputRegistry.class.getName(), Level.INFO);

        assertNull(result);
        assertEquals(0, outputRegistry.getRunningMessageOutputs().size());
    }
}
