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

import com.google.common.collect.Maps;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;

import javax.inject.Inject;
import java.util.Map;

public class MessageInputFactory {
    private final Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories;

    @Inject
    public MessageInputFactory(Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories) {
        this.inputFactories = inputFactories;
    }

    public MessageInput create(String type, Configuration configuration) throws NoSuchInputTypeException {
        if (inputFactories.containsKey(type)) {
            final MessageInput.Factory<? extends MessageInput> factory = inputFactories.get(type);
            return factory.create(configuration);
        }
        throw new NoSuchInputTypeException("There is no input of type <" + type + "> registered.");
    }

    public MessageInput create(InputCreateRequest lr, String user, String nodeId) throws NoSuchInputTypeException {
        final MessageInput input = create(lr.type(), new Configuration(lr.configuration()));
        input.setTitle(lr.title());
        input.setGlobal(lr.global());
        input.setCreatorUserId(user);
        input.setCreatedAt(Tools.nowUTC());
        if (!lr.global())
            input.setNodeId(nodeId);

        return input;
    }

    public Map<String, InputDescription> getAvailableInputs() {
        final Map<String, InputDescription> result = Maps.newHashMap();
        for (final Map.Entry<String, MessageInput.Factory<? extends MessageInput>> factories : inputFactories.entrySet()) {
            final MessageInput.Factory<? extends MessageInput> factory = factories.getValue();
            final MessageInput.Descriptor descriptor = factory.getDescriptor();
            final MessageInput.Config config = factory.getConfig();
            final InputDescription inputDescription = new InputDescription(descriptor, config);
            result.put(factories.getKey(), inputDescription);
        }

        return result;
    }
}
