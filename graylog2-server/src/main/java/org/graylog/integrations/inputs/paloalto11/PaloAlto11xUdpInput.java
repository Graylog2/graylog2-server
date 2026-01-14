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
package org.graylog.integrations.inputs.paloalto11;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import jakarta.inject.Inject;
import org.graylog2.inputs.transports.UdpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

public class PaloAlto11xUdpInput extends MessageInput {

    public static final String NAME = "Palo Alto Networks UDP (PAN-OS v11+)";

    @AssistedInject
    public PaloAlto11xUdpInput(MetricRegistry metricRegistry,
                               @Assisted Configuration configuration,
                               UdpTransport.Factory udpTransportFactory,
                               PaloAlto11xCodec.Factory codecFactory,
                               LocalMetricRegistry localRegistry,
                               Config config,
                               Descriptor descriptor,
                               ServerStatus serverStatus) {
        super(metricRegistry, configuration, udpTransportFactory.create(configuration), localRegistry,
                codecFactory.create(configuration), config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<PaloAlto11xUdpInput> {
        @Override
        PaloAlto11xUdpInput create(Configuration configuration);

        @Override
        PaloAlto11xUdpInput.Config getConfig();

        @Override
        PaloAlto11xUdpInput.Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {

        @Inject
        public Config(UdpTransport.Factory transport, PaloAlto11xCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
