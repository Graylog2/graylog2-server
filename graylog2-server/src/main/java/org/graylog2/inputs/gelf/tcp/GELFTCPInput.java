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
package org.graylog2.inputs.gelf.tcp;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.GelfCodec;
import org.graylog2.inputs.transports.TcpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

import javax.inject.Inject;

public class GELFTCPInput extends MessageInput {

    private static final String NAME = "GELF TCP";

    @AssistedInject
    public GELFTCPInput(MetricRegistry metricRegistry,
                        @Assisted Configuration configuration,
                        TcpTransport.Factory tcpFactory,
                        GelfCodec.Factory gelfCodecFactory,
                        LocalMetricRegistry localRegistry,
                        Config config,
                        Descriptor descriptor, ServerStatus serverStatus) {
        super(metricRegistry, configuration, tcpFactory.create(overrideDelimiter(configuration)), localRegistry, gelfCodecFactory.create(configuration),
              config, descriptor, serverStatus);
    }

    // Make sure that delimiter is null-byte for GELF. This is needed to support setups where the GELF TCP input
    // has been created with the wrong value.
    private static Configuration overrideDelimiter(Configuration configuration) {
        configuration.setBoolean(TcpTransport.CK_USE_NULL_DELIMITER, true);

        return configuration;
    }

    public interface Factory extends MessageInput.Factory<GELFTCPInput> {
        @Override
        GELFTCPInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    public static class Config extends MessageInput.Config {
        @Inject
        public Config(TcpTransport.Factory transport, GelfCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
