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
package org.graylog2.inputs.transports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.GeneratorTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.rateDeviation;

public class RandomMessageTransport extends GeneratorTransport {
    private static final Logger log = LoggerFactory.getLogger(RandomMessageTransport.class);

    public static final String CK_SOURCE = "source";
    public static final String CK_SLEEP = "sleep";
    public static final String CK_SLEEP_DEVIATION_PERCENT = "sleep_deviation";

    private final Random rand = new Random();
    private final FakeHttpRawMessageGenerator generator;
    private final int sleepMs;
    private final int maxSleepDeviation;
    private final ObjectMapper objectMapper;

    @AssistedInject
    public RandomMessageTransport(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        generator = new FakeHttpRawMessageGenerator(configuration.getString(CK_SOURCE));
        sleepMs = configuration.intIsSet(CK_SLEEP) ? configuration.getInt(CK_SLEEP) : 0;
        maxSleepDeviation =  configuration.intIsSet(CK_SLEEP_DEVIATION_PERCENT) ? configuration.getInt(CK_SLEEP_DEVIATION_PERCENT) : 0;
    }

    @Override
    protected RawMessage produceRawMessage(MessageInput input) {
        final byte[] payload;
        try {
            final FakeHttpRawMessageGenerator.GeneratorState state = generator.generateState();
            payload = objectMapper.writeValueAsBytes(state);

            final RawMessage raw = new RawMessage("randomhttp", input.getId(), null, payload);

            sleepUninterruptibly(rateDeviation(sleepMs, maxSleepDeviation, rand), MILLISECONDS);
            return raw;
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize generator state", e);
        }
        return null;
    }


    @FactoryClass
    public interface Factory extends Transport.Factory<RandomMessageTransport> {
        @Override
        RandomMessageTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends GeneratorTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest c = super.getRequestedConfiguration();
            c.addField(new NumberField(
                    CK_SLEEP,
                    "Sleep time",
                    25,
                    "How many milliseconds to sleep between generating messages.",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE
            ));

            c.addField(new NumberField(
                    CK_SLEEP_DEVIATION_PERCENT,
                    "Maximum random sleep time deviation",
                    30,
                    "The deviation is used to generate a more realistic and non-steady message flow.",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE
            ));

            c.addField(new TextField(
                    CK_SOURCE,
                    "Source name",
                    "example.org",
                    "What to use as source of the generate messages.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return c;
        }
    }
}
