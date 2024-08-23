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
import jakarta.inject.Inject;
import org.graylog2.inputs.transports.SyslogTcpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

public class PaloAlto11xInput extends MessageInput {

    public static final String NAME = "Palo Alto Networks TCP (PAN-OS v11+)";

    @Inject
    public PaloAlto11xInput(@Assisted Configuration configuration,
                            MetricRegistry metricRegistry,
                            SyslogTcpTransport.Factory transport,
                            LocalMetricRegistry localRegistry,
                            PaloAlto11xCodec.Factory codec,
                            PaloAlto11xInput.Config config,
                            PaloAlto11xInput.Descriptor descriptor,
                            ServerStatus serverStatus) {
        super(
                metricRegistry,
                configuration,
                transport.create(configuration),
                localRegistry,
                codec.create(configuration),
                config,
                descriptor,
                serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<PaloAlto11xInput> {
        @Override
        PaloAlto11xInput create(Configuration configuration);

        @Override
        PaloAlto11xInput.Config getConfig();

        @Override
        PaloAlto11xInput.Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {

        @Inject
        public Config(SyslogTcpTransport.Factory transport, PaloAlto11xCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
