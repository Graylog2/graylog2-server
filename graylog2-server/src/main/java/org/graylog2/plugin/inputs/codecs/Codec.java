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
package org.graylog2.plugin.inputs.codecs;

import org.graylog2.plugin.AbstractDescriptor;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.journal.RawMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Codec {
    @Nullable
    Message decode(@Nonnull RawMessage rawMessage);

    @Nullable
    CodecAggregator getAggregator();

    String getName();

    @Nonnull
    Configuration getConfiguration();

    interface Factory<C> {
        C create(Configuration configuration);
        Config getConfig();
        Descriptor getDescriptor();
    }

    interface Config {
        String CK_OVERRIDE_SOURCE = "override_source";

        ConfigurationRequest getRequestedConfiguration();
        void overrideDefaultValues(@Nonnull ConfigurationRequest cr);
    }

    class Descriptor extends AbstractDescriptor {
        public Descriptor() {
            // We ensure old Codec plugins remain compatible by setting an empty name in here
            this("");
        }

        protected Descriptor(String name) {
            super(name, false, "");
        }
    }
}
