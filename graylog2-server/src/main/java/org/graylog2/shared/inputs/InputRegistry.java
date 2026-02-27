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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.events.inputs.IOStateChangedEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Singleton
public class InputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);

    private final InputStateCache cache = new InputStateCache();

    @Inject
    public InputRegistry(EventBus eventBus) {
        eventBus.register(this);
    }

    @Subscribe
    public void onIOStateChanged(IOStateChangedEvent<MessageInput> event) {
        final IOState<MessageInput> changedState = event.changedState();
        final String inputId = changedState.getStoppable().getId();

        if (!cache.contains(inputId)) {
            return;
        }

        cache.updateState(inputId, event.oldState(), event.newState());
    }

    public Map<String, String> getStatusesByInputId() {
        return cache.getStatusesByInputId();
    }

    public Set<String> getInputIdsByState(IOState.Type state) {
        return cache.getIdsByState(state);
    }

    public Set<IOState<MessageInput>> getInputStates() {
        return ImmutableSet.copyOf(cache.getAll());
    }

    public IOState<MessageInput> getInputState(String inputId) {
        return cache.get(inputId);
    }

    public Set<IOState<MessageInput>> getRunningInputs() {
        Set<String> runningIds = cache.getIdsByState(IOState.Type.RUNNING);
        ImmutableSet.Builder<IOState<MessageInput>> builder = ImmutableSet.builder();
        for (String id : runningIds) {
            IOState<MessageInput> state = cache.get(id);
            if (state != null) {
                builder.add(state);
            }
        }
        return builder.build();
    }

    public boolean hasTypeRunning(Class klazz) {
        for (IOState<MessageInput> inputState : cache.getAll()) {
            if (inputState.getStoppable().getClass().equals(klazz)) {
                return true;
            }
        }
        return false;
    }

    public int runningCount() {
        return cache.getIdsByState(IOState.Type.RUNNING).size();
    }

    public boolean remove(MessageInput input) {
        final IOState<MessageInput> inputState = this.stop(input);
        input.terminate();
        if (inputState != null) {
            inputState.setState(IOState.Type.TERMINATED);
            cache.remove(input.getId());
        }
        return inputState != null;
    }

    public boolean remove(IOState<MessageInput> inputState) {
        final MessageInput messageInput = inputState.getStoppable();
        return remove(messageInput);
    }

    public IOState<MessageInput> stop(MessageInput input) {
        IOState<MessageInput> inputState = cache.get(input.getId());

        if (inputState != null) {
            inputState.setState(IOState.Type.STOPPING);
            try {
                input.stop();
            } catch (Exception e) {
                LOG.warn("Stopping input {} failed, removing anyway: {}", input.toIdentifier(), e);
            }
            inputState.setState(IOState.Type.STOPPED);
        }

        return inputState;
    }

    public void setup(IOState<MessageInput> inputState) {
        remove(inputState);
        inputState.setState(IOState.Type.SETUP);
        add(inputState);
    }

    public boolean add(IOState<MessageInput> messageInputIOState) {
        return cache.add(messageInputIOState);
    }

    public Stream<IOState<MessageInput>> stream() {
        return cache.getAll().stream();
    }

    class InputStateCache {
        private final ConcurrentHashMap<String, IOState<MessageInput>> byId = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<IOState.Type, Set<String>> byState = new ConcurrentHashMap<>();

        private IOState<MessageInput> get(String inputId) {
            return byId.get(inputId);
        }

        boolean contains(String inputId) {
            return byId.containsKey(inputId);
        }

        private Set<String> getIdsByState(IOState.Type state) {
            final Set<String> ids = byState.get(state);
            return ids != null ? Set.copyOf(ids) : Set.of();
        }

        private Collection<IOState<MessageInput>> getAll() {
            return byId.values();
        }

        private boolean add(IOState<MessageInput> ioState) {
            final String inputId = ioState.getStoppable().getId();
            final IOState<MessageInput> previous = byId.putIfAbsent(inputId, ioState);
            if (previous != null) {
                return false;
            }
            byState.computeIfAbsent(ioState.getState(), k -> ConcurrentHashMap.newKeySet())
                    .add(inputId);
            return true;
        }

        private IOState<MessageInput> remove(String inputId) {
            final IOState<MessageInput> removed = byId.remove(inputId);
            if (removed != null) {
                for (Set<String> ids : byState.values()) {
                    ids.remove(inputId);
                }
            }
            return removed;
        }

        private void updateState(String inputId, IOState.Type oldState, IOState.Type newState) {
            final Set<String> oldSet = byState.get(oldState);
            if (oldSet != null) {
                oldSet.remove(inputId);
            }
            byState.computeIfAbsent(newState, k -> ConcurrentHashMap.newKeySet())
                    .add(inputId);
        }

        private Map<String, String> getStatusesByInputId() {
            final Map<String, String> result = new HashMap<>();
            for (Map.Entry<IOState.Type, Set<String>> entry : byState.entrySet()) {
                final String stateStr = entry.getKey().toString();
                for (String inputId : entry.getValue()) {
                    result.put(inputId, stateStr);
                }
            }
            return result;
        }
    }
}
