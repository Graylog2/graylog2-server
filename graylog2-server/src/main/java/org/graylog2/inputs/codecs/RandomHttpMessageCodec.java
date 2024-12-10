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
import jakarta.inject.Inject;
import org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState;

@Codec(name = "random-http-msg", displayName = "Random HTTP Message")
public class RandomHttpMessageCodec extends AbstractCodec {
    private static final Logger log = LoggerFactory.getLogger(RandomHttpMessageCodec.class);
    private final ObjectMapper objectMapper;
    private final MessageFactory messageFactory;

    @Inject
    public RandomHttpMessageCodec(@Assisted Configuration configuration, ObjectMapper objectMapper,
                                  MessageFactory messageFactory) {
        super(configuration);
        this.objectMapper = objectMapper;
        this.messageFactory = messageFactory;
    }

    @Override
    public Optional<Message> decodeSafe(@Nonnull RawMessage rawMessage) {
        if (!rawMessage.getCodecName().equals(getName())) {
            throw InputProcessingException.create(
                    "Cannot decode payload type %s, skipping message %s".formatted(rawMessage.getCodecName(), rawMessage.getId()),
                    rawMessage, new String(rawMessage.getPayload(), charset));
        }
        try {
            final GeneratorState state = objectMapper.readValue(rawMessage.getPayload(), GeneratorState.class);
            return Optional.of(FakeHttpRawMessageGenerator.generateMessage(messageFactory, state));
        } catch (Exception e) {
            throw InputProcessingException.create(
                    "Cannot decode message to class FakeHttpRawMessageGenerator.GeneratorState",
                    rawMessage, new String(rawMessage.getPayload(), charset));
        }
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }


    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<RandomHttpMessageCodec> {
        @Override
        RandomHttpMessageCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
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
            super(RandomHttpMessageCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
