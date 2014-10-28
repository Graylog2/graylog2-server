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
package org.graylog2.filters;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StaticFieldFilterTest {

    @Test
    public void testFilter() throws Exception {
        Message msg = new Message("hello", "junit", Tools.iso8601());

        FakeInput fakeInput = new FakeInput(mock(MetricRegistry.class),mock(Transport.class),
                                            mock(MetricRegistry.class),
                                            mock(Codec.class),
                                            mock(MessageInput.Config.class), mock(MessageInput.Descriptor.class));
        fakeInput.addStaticField("foo", "bar");

        msg.setSourceInput(fakeInput);

        StaticFieldFilter filter = new StaticFieldFilter();
        filter.filter(msg);

        assertEquals("hello", msg.getMessage());
        assertEquals("junit", msg.getSource());
        assertEquals("bar", msg.getField("foo"));
    }

    @Test
    public void testFilterIsNotOverwritingExistingKeys() throws Exception {
        Message msg = new Message("hello", "junit", Tools.iso8601());
        msg.addField("foo", "IWILLSURVIVE");

        FakeInput fakeInput = new FakeInput(mock(MetricRegistry.class),mock(Transport.class),
                                            mock(MetricRegistry.class),
                                            mock(Codec.class),
                                            mock(MessageInput.Config.class), mock(MessageInput.Descriptor.class));
        fakeInput.addStaticField("foo", "bar");

        msg.setSourceInput(fakeInput);

        StaticFieldFilter filter = new StaticFieldFilter();
        filter.filter(msg);

        assertEquals("hello", msg.getMessage());
        assertEquals("junit", msg.getSource());
        assertEquals("IWILLSURVIVE", msg.getField("foo"));
    }

    private class FakeInput extends MessageInput {

        public FakeInput(MetricRegistry metricRegistry,
                         Transport transport,
                         MetricRegistry localRegistry, Codec codec, Config config, Descriptor descriptor) {
            super(metricRegistry, transport, localRegistry, codec, config, descriptor);
        }


        @Override
        public void launch(Buffer processBuffer) throws MisfireException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void stop() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}
