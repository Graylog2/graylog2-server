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

import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageOutputFactoryTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs;

    private MessageOutputFactory messageOutputFactory;

    public MessageOutputFactoryTest() {
        this.availableOutputs = Maps.newHashMap();
    }

    @Before
    public void setUp() throws Exception {
        this.messageOutputFactory = new MessageOutputFactory(availableOutputs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExistentOutputType() throws MessageOutputConfigurationException {
        final String outputType = "non.existent";
        final Output output = mock(Output.class);
        when(output.getType()).thenReturn(outputType);
        final Stream stream = mock(Stream.class);
        final Configuration configuration = mock(Configuration.class);

        messageOutputFactory.fromStreamOutput(output, stream, configuration);
    }
}
