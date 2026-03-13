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
package org.graylog.collectors.input;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.collectors.input.transport.CollectorIngestHttpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

/**
 * An HTTP input for OpAMP-managed agents that auto-configures Ed25519 mTLS from the
 * OpAMP CA hierarchy. TLS is always enabled — the configuration is forced at construction
 * time so that the parent transport adds the TLS handler to the Netty pipeline.
 */
public class CollectorIngestHttpInput extends MessageInput {
    public static final String NAME = "Collector Ingest (HTTP)";

    @Inject
    public CollectorIngestHttpInput(MetricRegistry metricRegistry,
                              @Assisted Configuration configuration,
                              CollectorIngestHttpTransport.Factory transportFactory,
                              LocalMetricRegistry localRegistry,
                              CollectorIngestCodec.Factory codecFactory,
                              Config config,
                              Descriptor descriptor,
                              ServerStatus serverStatus) {
        super(metricRegistry, configuration, transportFactory.create(configuration),
                localRegistry, codecFactory.create(configuration), config, descriptor, serverStatus);
    }

    @Override
    public Boolean isGlobal() {
        return true;
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<CollectorIngestHttpInput> {
        @Override
        CollectorIngestHttpInput create(Configuration configuration);

        @Override
        CollectorIngestHttpInput.Config getConfig();

        @Override
        CollectorIngestHttpInput.Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(CollectorIngestHttpTransport.Factory transport, CollectorIngestCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
