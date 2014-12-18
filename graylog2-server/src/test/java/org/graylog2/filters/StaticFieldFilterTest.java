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
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class StaticFieldFilterTest {

    private FakeMessageCodecConfig fakeMessageCodecConfig;

    @BeforeMethod
    public void setUp() throws Exception {
        final Transport.Config transportConfig = mock(Transport.Config.class);
        final Codec.Config codecConfig = mock(Codec.Config.class);

        when(transportConfig.getRequestedConfiguration()).thenReturn(new ConfigurationRequest());
        when(codecConfig.getRequestedConfiguration()).thenReturn(new ConfigurationRequest());

        fakeMessageCodecConfig = new FakeMessageCodecConfig(transportConfig, codecConfig);
    }

    @Test
    public void testFilter() throws Exception {
        Message msg = new Message("hello", "junit", Tools.iso8601());

        FakeInput fakeInput = new FakeInput(mock(MetricRegistry.class), mock(Configuration.class), mock(Transport.class),
                mock(LocalMetricRegistry.class),
                mock(Codec.class),
                fakeMessageCodecConfig, mock(MessageInput.Descriptor.class), null);
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

        FakeInput fakeInput = new FakeInput(mock(MetricRegistry.class), mock(Configuration.class), mock(Transport.class),
                mock(LocalMetricRegistry.class),
                mock(Codec.class),
                fakeMessageCodecConfig, mock(MessageInput.Descriptor.class), null);
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
                         Configuration configuration,
                         Transport transport,
                         LocalMetricRegistry localRegistry, Codec codec, Config config, Descriptor descriptor, ServerStatus serverStatus) {
            super(metricRegistry, configuration, transport, localRegistry, codec, config, descriptor, serverStatus);
        }


        @Override
        public void launch(InputBuffer processBuffer) throws MisfireException {
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

    private class FakeMessageCodecConfig extends MessageInput.Config {
        protected FakeMessageCodecConfig(Transport.Config transportConfig, Codec.Config codecConfig) {
            super(transportConfig, codecConfig);
        }
    }
}
