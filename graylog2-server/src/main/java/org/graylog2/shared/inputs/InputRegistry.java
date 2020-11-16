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
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class InputRegistry extends HashSet<IOState<MessageInput>> {
    private static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);

    public InputRegistry() {
        super();
    }


    public Set<IOState<MessageInput>> getInputStates() {
        return ImmutableSet.copyOf(this);
    }

    public IOState<MessageInput> getInputState(String inputId) {
        for (IOState<MessageInput> inputState : this) {
            if (inputState.getStoppable().getPersistId().equals(inputId))
                return inputState;
        }

        return null;
    }

    public Set<IOState<MessageInput>> getRunningInputs() {
        ImmutableSet.Builder<IOState<MessageInput>> runningInputs = ImmutableSet.builder();
        for (IOState<MessageInput> inputState : this) {
            if (inputState.getState() == IOState.Type.RUNNING)
                runningInputs.add(inputState);
        }
        return runningInputs.build();
    }

    public boolean hasTypeRunning(Class klazz) {
        for (IOState<MessageInput> inputState : this) {
            if (inputState.getStoppable().getClass().equals(klazz)) {
                return true;
            }
        }

        return false;
    }

    public int runningCount() {
        return getRunningInputs().size();
    }

    public MessageInput getRunningInput(String inputId) {
        for (IOState<MessageInput> inputState : this) {
            if (inputState.getStoppable().getId().equals(inputId))
                return inputState.getStoppable();
        }

        return null;
    }

    public IOState<MessageInput> getRunningInputState(String inputStateId) {
        for (IOState<MessageInput> inputState : this) {
            if (inputState.getStoppable().getId().equals(inputStateId))
                return inputState;
        }

        return null;
    }

    public boolean remove(MessageInput input) {
        final IOState<MessageInput> inputState = this.stop(input);
        input.terminate();
        if (inputState != null)
            inputState.setState(IOState.Type.TERMINATED);

        return super.remove(inputState);
    }

    public boolean remove(IOState<MessageInput> inputState) {
        final MessageInput messageInput = inputState.getStoppable();
        return remove(messageInput);
    }

    public IOState<MessageInput> stop(MessageInput input) {
        IOState<MessageInput> inputState = getRunningInputState(input.getId());

        if (inputState != null) {
            inputState.setState(IOState.Type.STOPPING);
            try {
                input.stop();
            } catch (Exception e) {
                LOG.warn("Stopping input <{}> failed, removing anyway: {}", input.getId(), e);
            }
            inputState.setState(IOState.Type.STOPPED);
        }

        return inputState;
    }
}