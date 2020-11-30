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
package org.graylog2.plugin.outputs;

import org.graylog2.plugin.AbstractDescriptor;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Stoppable;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;

import java.util.List;

public interface MessageOutput extends Stoppable {
    // This factory is implemented by output plugins that have been built before Graylog 3.0.1.
    // We have to keep it around to make sure older plugins still load with Graylog >=3.0.1.
    // It can be removed once we decide to stop supporting old plugins.
    interface Factory<T> {
        T create(Stream stream, Configuration configuration);
        Config getConfig();
        Descriptor getDescriptor();
    }

    // This is the factory that should be implemented by output plugins which target Graylog 3.0.1 and later.
    // The only change compared to Factory is that it also takes the Output instance parameter.
    interface Factory2<T> {
        T create(Output output, Stream stream, Configuration configuration);
        Config getConfig();
        Descriptor getDescriptor();
    }

    class Descriptor extends AbstractDescriptor {
        private final String humanName;

        protected Descriptor() {
            throw new IllegalStateException("This class should not be instantiated directly, this is a bug.");
        }

        public Descriptor(String name, boolean exclusive, String linkToDocs, String humanName) {
            super(name, exclusive, linkToDocs);
            this.humanName = humanName;
        }

        public String getHumanName() {
            return humanName;
        }
    }

    class Config {
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }
    }

    boolean isRunning();
    void write(Message message) throws Exception;
    void write(List<Message> messages) throws Exception;
}
