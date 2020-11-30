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
package org.graylog2.inputs.misc.jsonpath;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.JsonPathCodec;
import org.graylog2.inputs.transports.HttpPollTransport;
import org.graylog2.plugin.DocsHelper;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

import javax.inject.Inject;

public class JsonPathInput extends MessageInput {

    private static final String NAME = "JSON path from HTTP API";

    @AssistedInject
    public JsonPathInput(@Assisted Configuration configuration,
                         HttpPollTransport.Factory transport,
                         JsonPathCodec.Factory codec,
                         MetricRegistry metricRegistry,
                         LocalMetricRegistry localRegistry, Config config, Descriptor descriptor, ServerStatus serverStatus) {
        super(metricRegistry, configuration, transport.create(configuration), localRegistry, codec.create(configuration), config,
              descriptor, serverStatus);
    }

    public interface Factory extends MessageInput.Factory<JsonPathInput> {
        @Override
        JsonPathInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, DocsHelper.PAGE_SENDING_JSONPATH.toString());
        }
    }


    public static class Config extends MessageInput.Config {
        @Inject
        public Config(HttpPollTransport.Factory transport, JsonPathCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
