/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.integrations.ipfix.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog.integrations.ipfix.codecs.IpfixCodec;
import org.graylog.integrations.ipfix.transports.IpfixUdpTransport;
import org.graylog2.plugin.DocsHelper;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class IpfixUdpInput extends MessageInput {
    private static final String NAME = "IPFIX UDP";

    @Inject
    public IpfixUdpInput(MetricRegistry metricRegistry,
                         @Assisted Configuration configuration,
                         IpfixUdpTransport transport,
                         LocalMetricRegistry localRegistry,
                         IpfixCodec codec,
                         Config config,
                         Descriptor descriptor,
                         ServerStatus serverStatus) {
        super(metricRegistry, configuration, transport, localRegistry, codec, config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<IpfixUdpInput> {
        @Override
        IpfixUdpInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, DocsHelper.PAGE_SENDING_IPFIXPATH.toString());
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(IpfixUdpTransport.Factory transport, IpfixCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }

}
