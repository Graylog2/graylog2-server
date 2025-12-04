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

import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class MessageOutputFactoryTest {

    private final Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs;
    private final Map<String, MessageOutput.Factory2<? extends MessageOutput>> availableOutputs2;

    private MessageOutputFactory messageOutputFactory;

    public MessageOutputFactoryTest() {
        this.availableOutputs = Maps.newHashMap();
        this.availableOutputs2 = Maps.newHashMap();
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.messageOutputFactory = new MessageOutputFactory(availableOutputs, availableOutputs2);
    }

    @Test
    public void testNonExistentOutputType() throws MessageOutputConfigurationException {
        assertThrows(IllegalArgumentException.class, () -> {
            final String outputType = "non.existent";
            final Output output = mock(Output.class);
            when(output.getType()).thenReturn(outputType);
            final Stream stream = mock(Stream.class);
            final Configuration configuration = mock(Configuration.class);

            messageOutputFactory.fromStreamOutput(output, stream, configuration);
        });
    }
}
