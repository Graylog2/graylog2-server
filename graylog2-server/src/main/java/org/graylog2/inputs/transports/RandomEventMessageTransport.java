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
package org.graylog2.inputs.transports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.random.generators.FakeEventMessageGenerator;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.GeneratorTransport;
import org.graylog2.plugin.inputs.transports.Transport;

public class RandomEventMessageTransport extends RandomMessageTransport {
    @AssistedInject
    public RandomEventMessageTransport(@Assisted  Configuration configuration, EventBus eventBus, ObjectMapper objectMapper) {
        super(configuration, eventBus, objectMapper);

        this.generator = new FakeEventMessageGenerator(configuration);
    }


    @FactoryClass
    public interface Factory extends Transport.Factory<RandomEventMessageTransport> {
        @Override
        RandomEventMessageTransport create(Configuration configuration);

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
