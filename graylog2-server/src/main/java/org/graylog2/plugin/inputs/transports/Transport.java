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
package org.graylog2.plugin.inputs.transports;

import com.codahale.metrics.MetricSet;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;

import javax.annotation.Nullable;
import java.util.Map;

public interface Transport {
    void setMessageAggregator(CodecAggregator aggregator);

    void launch(MessageInput input) throws MisfireException;

    default void launch(MessageInput input, @Nullable InputFailureRecorder inputFailureRecorder) throws MisfireException {
        launch(input);
    }

    void stop();

    MetricSet getMetricSet();

    interface Config {
        ConfigurationRequest getRequestedConfiguration();

        /**
         * Adds default values to the configuration of a newly created input before it is persisted.
         * <p>
         * Use this for settings whose value for new inputs differs from the value that applies when the
         * setting is missing in the configuration of an existing input. (e.g., a setting that has been added later
         * and must not change the behavior of existing inputs)
         *
         * @param configuration the configuration of the new input, must not be modified
         * @return the configuration to persist
         */
        default Map<String, Object> withNewInputDefaults(Map<String, Object> configuration) {
            return configuration;
        }

        /**
         * Adds default values to the configuration of a persisted input when it is read.
         * <p>
         * Use this to make the effective value of settings that are missing in the persisted configuration visible
         * to API clients. (e.g., the web interface fills missing settings with the field's default value when
         * editing an input, which would change the behavior of existing inputs)
         *
         * @param configuration the persisted configuration of the input, must not be modified
         * @return the configuration to return to callers
         */
        default Map<String, Object> withStoredInputDefaults(Map<String, Object> configuration) {
            return configuration;
        }
    }

    interface Factory<T extends Transport> {
        T create(Configuration configuration);

        Config getConfig();
    }
}
