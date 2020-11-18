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
package org.graylog.plugins.beats;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class Beats2Input extends MessageInput {
    private static final String NAME = "Beats";

    @Inject
    public Beats2Input(@Assisted Configuration configuration,
                       BeatsTransport.Factory transportFactory,
                       Beats2Codec.Factory codecFactory,
                       Config config,
                       Descriptor descriptor,
                       MetricRegistry metricRegistry,
                       LocalMetricRegistry localRegistry,
                       ServerStatus serverStatus) {
        super(metricRegistry, configuration, transportFactory.create(configuration),
                localRegistry, codecFactory.create(configuration), config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<Beats2Input> {
        @Override
        Beats2Input create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(BeatsTransport.Factory transport, Beats2Codec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
