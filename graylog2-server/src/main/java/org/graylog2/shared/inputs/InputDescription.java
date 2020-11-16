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
package org.graylog2.shared.inputs;

import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;

import java.util.Map;

public class InputDescription {
    private final MessageInput.Descriptor descriptor;
    private final MessageInput.Config config;

    public InputDescription(MessageInput.Descriptor descriptor, MessageInput.Config config) {
        this.descriptor = descriptor;
        this.config = config;
    }

    public String getName() {
        return descriptor.getName();
    }

    public boolean isExclusive() {
        return descriptor.isExclusive();
    }

    public String getLinkToDocs() {
        return descriptor.getLinkToDocs();
    }

    public Map<String, Map<String, Object>> getRequestedConfiguration() {
        return config.combinedRequestedConfiguration().asList();
    }

    public ConfigurationRequest getConfigurationRequest() {
        return config.combinedRequestedConfiguration();
    }
}
