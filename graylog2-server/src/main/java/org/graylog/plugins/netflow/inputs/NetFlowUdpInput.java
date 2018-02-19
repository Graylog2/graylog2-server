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
package org.graylog.plugins.netflow.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.netflow.codecs.NetFlowCodec;
import org.graylog.plugins.netflow.transport.NetFlowUdpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class NetFlowUdpInput extends MessageInput {
    private static final String NAME = "NetFlow UDP";

    @Inject
    public NetFlowUdpInput(MetricRegistry metricRegistry,
                           @Assisted Configuration configuration,
                           NetFlowUdpTransport.Factory transportFactory,
                           NetFlowCodec.Factory codecFactory,
                           LocalMetricRegistry localMetricRegistry,
                           Config config,
                           Descriptor descriptor,
                           ServerStatus serverStatus) {
        super(metricRegistry, configuration, transportFactory.create(configuration), localMetricRegistry,
                codecFactory.create(configuration), config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<NetFlowUdpInput> {
        @Override
        NetFlowUdpInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "https://github.com/Graylog2/graylog-plugin-netflow");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(NetFlowUdpTransport.Factory transport, NetFlowCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
