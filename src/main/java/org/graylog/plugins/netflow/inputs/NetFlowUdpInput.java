/**
 * Copyright (C) 2012, 2013, 2014 wasted.io Ltd <really@wasted.io>
 * Copyright (C) 2015 Graylog, Inc. (hello@graylog.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.netflow.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.netflow.codecs.NetFlowCodec;
import org.graylog2.inputs.transports.UdpTransport;
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
                           UdpTransport.Factory transportFactory,
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
        public Config(UdpTransport.Factory transport, NetFlowCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
