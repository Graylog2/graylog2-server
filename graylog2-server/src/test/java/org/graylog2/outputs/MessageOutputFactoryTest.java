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

import autovalue.shaded.com.google.common.common.collect.Maps;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.bindings.InstantiationService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

@Test
public class MessageOutputFactoryTest {
    @Mock
    private InstantiationService instantiationService;
    private final Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs;

    private MessageOutputFactory messageOutputFactory;

    public MessageOutputFactoryTest() {
        this.availableOutputs = Maps.newHashMap();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.messageOutputFactory = new MessageOutputFactory(instantiationService, availableOutputs);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNonExistentOutputType() throws MessageOutputConfigurationException {
        final String outputType = "non.existent";
        final Output output = mock(Output.class);
        when(output.getType()).thenReturn(outputType);
        final Stream stream = mock(Stream.class);
        final Configuration configuration = mock(Configuration.class);

        messageOutputFactory.fromStreamOutput(output, stream, configuration);
    }
}