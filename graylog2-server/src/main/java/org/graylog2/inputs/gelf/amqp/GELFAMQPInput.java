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
package org.graylog2.inputs.gelf.amqp;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.GelfCodec;
import org.graylog2.inputs.transports.AmqpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

import javax.inject.Inject;

public class GELFAMQPInput extends MessageInput {

    private static final String NAME = "GELF AMQP";

    @AssistedInject
    public GELFAMQPInput(final MetricRegistry metricRegistry,
                         final @Assisted Configuration configuration,
                         final AmqpTransport.Factory amqpFactory,
                         final GelfCodec.Factory gelfCodecFactory,
                         final LocalMetricRegistry localRegistry,
                         final Config config,
                         final Descriptor descriptor,
                         final ServerStatus serverStatus) {
        super(metricRegistry, configuration, amqpFactory.create(configuration), localRegistry,
                gelfCodecFactory.create(configuration), config, descriptor, serverStatus);
    }

    public interface Factory extends MessageInput.Factory<GELFAMQPInput> {
        @Override
        GELFAMQPInput create(Configuration configuration);

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
        public Config(AmqpTransport.Factory transport, GelfCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
