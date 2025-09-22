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


import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Singleton
public class InputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);

    private final ConcurrentMap<String, IOState<MessageInput>> inputStates = new ConcurrentHashMap<>();
    private final IOState.Factory<MessageInput> inputStateFactory;
    private final FeatureFlags featureFlags;

    @Inject
    public InputRegistry(IOState.Factory<MessageInput> inputStateFactory, FeatureFlags featureFlags) {
        this.inputStateFactory = inputStateFactory;
        this.featureFlags = featureFlags;
    }

    public Set<IOState<MessageInput>> getInputStates() {
        return ImmutableSet.copyOf(inputStates.values());
    }

    @Nullable
    public IOState<MessageInput> getInputState(String inputId) {
        return inputStates.get(inputId);
    }

    public Set<IOState<MessageInput>> getRunningInputs() {
        ImmutableSet.Builder<IOState<MessageInput>> runningInputs = ImmutableSet.builder();
        for (IOState<MessageInput> inputState : inputStates.values()) {
            if (inputState.getState() == IOState.Type.RUNNING) {
                runningInputs.add(inputState);
            }
        }
        return runningInputs.build();
    }

    public boolean hasTypeRunning(Class klazz) {
        for (IOState<MessageInput> inputState : inputStates.values()) {
            if (inputState.getStoppable().getClass().equals(klazz)) {
                return true;
            }
        }

        return false;
    }

    public int runningCount() {
        return getRunningInputs().size();
    }

    @Nullable
    public MessageInput getRunningInput(String inputId) {
        final IOState<MessageInput> state = inputStates.get(inputId);

        return state != null ? state.getStoppable() : null;
    }

    @Nullable
    public IOState<MessageInput> getRunningInputState(String inputStateId) {
        return inputStates.get(inputStateId);
    }

    public boolean remove(String inputId) {
        final var inputState = inputStates.get(inputId);
        if (inputState != null) {
            return remove(inputState);
        }
        return false;
    }

    public boolean remove(IOState<MessageInput> inputState) {
        return remove(inputState.getStoppable());
    }

    public boolean remove(MessageInput input) {
        stop(input);
        return inputStates.remove(input.getId()) != null;
    }

    public IOState<MessageInput> stop(MessageInput input) {
        final var inputState = inputStates.get(input.getId());

        if (inputState != null) {
            try {
                inputState.triggerStop();
            } catch (Exception e) {
                LOG.warn("Stopping input {} failed, removing anyway: {}", input.toIdentifier(), e);
            }
        }

        return inputState;
    }

    public void setup(String inputId) {
        final IOState<MessageInput> inputState = inputStates.get(inputId);
        if (inputState != null) {
            inputState.triggerSetup();
            remove(inputState);
        }
    }

    private IOState<MessageInput> createInputState(MessageInput messageInput) {
        if (featureFlags.isOn("SETUP_MODE") && messageInput.getDesiredState() == IOState.Type.SETUP) {
            return inputStateFactory.create(messageInput, IOState.Type.SETUP);
        }
        return inputStateFactory.create(messageInput);
    }

    public IOState<MessageInput> add(MessageInput messageInput) {
        return inputStates.merge(
                messageInput.getId(),
                createInputState(messageInput),
                (existingState, newSate) -> {
                    switch (existingState.getState()) {
                        case RUNNING, STARTING, FAILING -> {
                        }
                        default -> existingState.setStoppable(messageInput);
                    }
                    return existingState;
                }
        );
    }

    public Stream<IOState<MessageInput>> stream() {
        return inputStates.values().stream();
    }

    private MessageInput getMessageInput(IOState<MessageInput> inputState) {
        return requireNonNull(inputState.getStoppable(), "IOState#stoppable cannot be null");
    }
}
