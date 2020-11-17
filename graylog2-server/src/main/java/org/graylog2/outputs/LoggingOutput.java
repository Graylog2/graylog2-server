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
package org.graylog2.outputs;

import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoggingOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingOutput.class);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Configuration configuration;

    @Inject
    public LoggingOutput(@Assisted Configuration config) throws MessageOutputConfigurationException {
        LOG.info("Initializing");
        configuration = config;
        isRunning.set(true);
    }

    @Override
    public void stop() {
        isRunning.set(false);
        LOG.info("Stopping");
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        LOG.info("{} {}", configuration.getString("prefix"), message);
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }
    }

    public interface Factory extends MessageOutput.Factory<LoggingOutput> {
        @Override
        LoggingOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("STDOUT Output", false, "", "An output writing every message to STDOUT.");
        }
    }


    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ConfigurationRequest configurationRequest = new ConfigurationRequest();
            configurationRequest.addField(new TextField("prefix", "Prefix", "Writing message: ", "How to prefix the message before logging it", ConfigurationField.Optional.OPTIONAL));
            return configurationRequest;
        }
    }
}
