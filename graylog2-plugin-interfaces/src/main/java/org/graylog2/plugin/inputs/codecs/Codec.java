/*
 * Copyright 2014 TORCH GmbH
 *
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
package org.graylog2.plugin.inputs.codecs;

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

    public interface Factory<C> {
        C create(Configuration configuration);
        Config getConfig();
    }

    public interface Config {
        ConfigurationRequest getRequestedConfiguration();
        void overrideDefaultValues(@Nonnull ConfigurationRequest cr);
    }
}
