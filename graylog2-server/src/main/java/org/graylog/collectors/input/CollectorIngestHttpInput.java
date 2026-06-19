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
 * The only managed ingest input for OpAMP-managed agents. It auto-configures Ed25519 mTLS
 * from the OpAMP CA hierarchy, and TLS is always enabled so the parent transport adds the
 * TLS handler to the Netty pipeline at construction time. See PR #24815:
 * https://github.com/Graylog2/graylog2-server/pull/24815
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

        @Override
        public String getDescription() {
            return "This input receives data from managed collectors over mTLS-secured HTTP. "
                    + "Managed collectors are configured to send their data to the external address "
                    + "specified in the Collectors Settings page. The port configured on this input must "
                    + "either match the external port from the Collectors Settings, or the external port "
                    + "must be routed to this input's port (e.g. via a load balancer or port mapping). "
                    + "Changing this input's port without updating the routing will prevent collectors "
                    + "from delivering data.";
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
