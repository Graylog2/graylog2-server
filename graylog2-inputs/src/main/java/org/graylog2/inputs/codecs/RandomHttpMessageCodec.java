/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState;

public class RandomHttpMessageCodec implements Codec {
    private static final Logger log = LoggerFactory.getLogger(RandomHttpMessageCodec.class);
    private final ObjectMapper objectMapper;

    @Inject
    public RandomHttpMessageCodec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        if (!rawMessage.getPayloadType().equals(getName())) {
            log.error("Cannot decode payload type {}, skipping message {}", rawMessage.getPayloadType(), rawMessage.getId());
            return null;
        }
        try {
            final GeneratorState state = objectMapper.readValue(rawMessage.getPayload(), GeneratorState.class);
            final Message message = FakeHttpRawMessageGenerator.generateMessage(state);
            return message;
        } catch (IOException e) {
            log.error("Cannot decode message to class FakeHttpRawMessageGenerator.GeneratorState", e);
        }
        return null;
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return "randomhttp";
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<RandomHttpMessageCodec> {
        @Override
        RandomHttpMessageCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }

}
