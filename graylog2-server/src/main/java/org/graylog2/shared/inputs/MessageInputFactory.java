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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class MessageInputFactory {
    private final Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories;

    private final FeatureFlags featureFlags;

    private final ConcurrentMap<String, Boolean> onlyOnePerClusterByType = new ConcurrentHashMap<>();

    @Inject
    public MessageInputFactory(Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories,
                               FeatureFlags featureFlags) {
        this.inputFactories = inputFactories;
        this.featureFlags = featureFlags;
    }

    public MessageInput create(String type, Configuration configuration) throws NoSuchInputTypeException {
        if (inputFactories.containsKey(type)) {
            final MessageInput.Factory<? extends MessageInput> factory = inputFactories.get(type);
            return factory.create(configuration);
        }
        throw new NoSuchInputTypeException("There is no input of type <" + type + "> registered.");
    }

    public MessageInput create(InputCreateRequest lr, String user, String nodeId) throws NoSuchInputTypeException {
        return create(lr, user, nodeId, false);
    }

    public MessageInput create(InputCreateRequest lr, String user, String nodeId, boolean isSetupWizard) throws NoSuchInputTypeException {
        final MessageInput input = create(lr.type(), new Configuration(lr.configuration()));
        input.setTitle(lr.title());
        input.setGlobal(lr.global());
        input.setCreatorUserId(user);
        input.setCreatedAt(Tools.nowUTC());
        if (!lr.global()) {
            input.setNodeId(nodeId);
        }

        if (featureFlags.isOn("SETUP_MODE") && input.supportsSetupMode() && isSetupWizard) {
            input.setDesiredState(IOState.Type.SETUP);
        }

        return input;
    }

    public MessageInput create(InputCreateRequest lr, String user, String nodeId, IOState.Type state) throws NoSuchInputTypeException {
        final MessageInput input = create(lr, user, nodeId);
        input.setDesiredState(state);
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

    /**
     * {@link MessageInput#onlyOnePerCluster()} is an instance method, but every implementation returns a constant, so
     * the answer only needs one instance per type rather than one per call.
     */
    public boolean onlyOnePerCluster(String type, Configuration configuration) throws NoSuchInputTypeException {
        final Boolean known = onlyOnePerClusterByType.get(type);
        if (known != null) {
            return known;
        }
        final boolean value = create(type, configuration).onlyOnePerCluster();
        onlyOnePerClusterByType.put(type, value);
        return value;
    }

    public Optional<MessageInput.Config> getConfig(String type) {
        if (inputFactories.containsKey(type)) {
            return Optional.of(inputFactories.get(type).getConfig());
        }
        return Optional.empty();
    }
}
