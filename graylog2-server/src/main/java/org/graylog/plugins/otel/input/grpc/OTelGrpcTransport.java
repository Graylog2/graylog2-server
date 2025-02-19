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
package org.graylog.plugins.otel.input.grpc;

import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import io.grpc.ServerServiceDefinition;
import jakarta.inject.Inject;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.security.encryption.EncryptedValueService;

import java.util.List;

public class OTelGrpcTransport extends AbstractGrpcTransport {
    public static final String NAME = "GrpcTransport";

    private final OTelLogsService.Factory logsServiceFactory;

    @Inject
    public OTelGrpcTransport(EventBus eventBus,
                             @Assisted Configuration configuration,
                             LocalMetricRegistry localMetricRegistry,
                             OTelLogsService.Factory logsServiceFactory,
                             EncryptedValueService encryptedValueService) {
        super(eventBus, configuration, localMetricRegistry, encryptedValueService);
        this.logsServiceFactory = logsServiceFactory;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<OTelGrpcTransport> {
        @Override
        OTelGrpcTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractGrpcTransport.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return super.getRequestedConfiguration();
        }
    }

    @Override
    List<ServerServiceDefinition> grpcServices(MessageInput input) {
        return List.of(logsServiceFactory.create(this, input).bindService());
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
    }
}
