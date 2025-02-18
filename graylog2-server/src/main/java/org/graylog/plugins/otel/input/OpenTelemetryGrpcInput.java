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
package org.graylog.plugins.otel.input;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.plugins.otel.input.codec.OpenTelemetryCodec;
import org.graylog.plugins.otel.input.grpc.OpenTelemetryGrpcTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

public class OpenTelemetryGrpcInput extends MessageInput {
    public static final String NAME = "OpenTelemetry (gRPC)";

    @Inject
    public OpenTelemetryGrpcInput(MetricRegistry metricRegistry,
                                  @Assisted Configuration configuration,
                                  OpenTelemetryGrpcTransport transport,
                                  LocalMetricRegistry localRegistry,
                                  OpenTelemetryCodec codec,
                                  Config config,
                                  Descriptor descriptor,
                                  ServerStatus serverStatus) {
        super(metricRegistry, configuration, transport, localRegistry, codec, config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<OpenTelemetryGrpcInput> {
        @Override
        OpenTelemetryGrpcInput create(Configuration configuration);

        @Override
        OpenTelemetryGrpcInput.Config getConfig();

        @Override
        OpenTelemetryGrpcInput.Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {

        @Inject
        public Config(OpenTelemetryGrpcTransport.Factory transport, OpenTelemetryCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
