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
package org.graylog.inputs.otel;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.inputs.otel.codec.OTelCodec;
import org.graylog.inputs.otel.transport.OTelHttpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

public class OTelHttpInput extends MessageInput {
    public static final String NAME = "OpenTelemetry (HTTP)";

    @Inject
    public OTelHttpInput(MetricRegistry metricRegistry,
                         @Assisted Configuration configuration,
                         OTelHttpTransport.Factory transportFactory,
                         LocalMetricRegistry localRegistry,
                         OTelCodec.Factory codecFactory,
                         Config config,
                         Descriptor descriptor,
                         ServerStatus serverStatus) {
        super(metricRegistry, configuration, transportFactory.create(configuration), localRegistry,
                codecFactory.create(configuration), config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<OTelHttpInput> {
        @Override
        OTelHttpInput create(Configuration configuration);

        @Override
        OTelHttpInput.Config getConfig();

        @Override
        OTelHttpInput.Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(OTelHttpTransport.Factory transport, OTelCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
