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
package org.graylog2.inputs.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.inputs.transports.InternalMetrics;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;

@Codec(name = "graylog-internal-metrics", displayName = "Graylog Internal Metrics")
public class InternalMetricsCodec extends AbstractCodec {

    private static final Logger log = LoggerFactory.getLogger(InternalMetricsCodec.class);

    private final ObjectMapper objectMapper;

    @Inject
    public InternalMetricsCodec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        super(configuration);
        this.objectMapper = objectMapper;
    }
    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            final InternalMetrics state = objectMapper.readValue(rawMessage.getPayload(), InternalMetrics.class);
            return createMessage(state);
        } catch (IOException e) {
            log.error("Failed to decode internal metrics message", e);
            return null;
        }
    }

    private Message createMessage(InternalMetrics state) {
        final Message message = new Message("Internal metrics", "MetricsFactory", state.timestamp());
        state.gauges().forEach(message::addField);
        return message;
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }



    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<InternalMetricsCodec> {
        @Override
        InternalMetricsCodec create(Configuration configuration);

        @Override
        InternalMetricsCodec.Config getConfig();

        @Override
        InternalMetricsCodec.Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(InternalMetricsCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
