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
package org.graylog.inputs.state;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.transitions.Transition;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters1;
import com.google.common.eventbus.EventBus;
import jakarta.inject.Inject;
import org.graylog.inputs.MessageInputFailure;
import org.graylog.inputs.MessageInputLifecycle;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.events.inputs.IOStateChangedEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class MessageInputStateMachine extends StateMachine<IOState.Type, IOState.Trigger> {
    private static final Logger LOG = LoggerFactory.getLogger(MessageInputStateMachine.class);

    private final TriggerWithParameters1<MessageInputFailure, IOState.Trigger> failTrigger;

    public MessageInputStateMachine(IOState.Type initialState,
                                    StateMachineConfig<IOState.Type, IOState.Trigger> config,
                                    TriggerWithParameters1<MessageInputFailure, IOState.Trigger> failTrigger) {
        super(initialState, config);
        this.failTrigger = failTrigger;
    }

    public TriggerWithParameters1<MessageInputFailure, IOState.Trigger> failTrigger() {
        return failTrigger;
    }

    public static class Factory {
        private final MessageInputLifecycle.Factory lifecycleFactory;
        private final EventBus eventBus;

        @Inject
        public Factory(MessageInputLifecycle.Factory lifecycleFactory, EventBus eventBus) {
            this.lifecycleFactory = lifecycleFactory;
            this.eventBus = eventBus;
        }

        public MessageInputStateMachine create(IOState.Type initialState, IOState<MessageInput> inputState, MessageInput messageInput) {
            final var inputLifecycle = lifecycleFactory.create(messageInput);
            final var config = new StateMachineConfig<IOState.Type, IOState.Trigger>();

            final var failTrigger = config.setTriggerParameters(IOState.Trigger.FAIL, MessageInputFailure.class);

            final Consumer<Transition<IOState.Type, IOState.Trigger>> trace = t -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} - [{}] {} => {}", messageInput.toIdentifier(), t.getTrigger(), t.getSource(), t.getDestination());
                }
                eventBus.post(IOStateChangedEvent.create(t.getSource(), t.getDestination(), inputState));
            };

            config.configure(IOState.Type.CREATED)
                    .permit(IOState.Trigger.START, IOState.Type.STARTING, () -> {})
                    .permit(IOState.Trigger.SETUP, IOState.Type.SETUP)
                    .onEntry(trace::accept)
                    .ignore(IOState.Trigger.RUNNING)
                    .ignore(IOState.Trigger.FAIL)
                    .ignore(IOState.Trigger.STOP)
                    .ignore(IOState.Trigger.STOPPED)
                    .ignore(IOState.Trigger.TERMINATE);

            config.configure(IOState.Type.SETUP)
                    .onEntry(trace::accept)
                    .onEntry(() -> inputLifecycle.stop(inputState))
                    .ignore(IOState.Trigger.START)
                    .ignore(IOState.Trigger.RUNNING)
                    .ignore(IOState.Trigger.FAIL)
                    .ignore(IOState.Trigger.STOP)
                    .ignore(IOState.Trigger.STOPPED)
                    .ignore(IOState.Trigger.TERMINATE)
                    .ignore(IOState.Trigger.SETUP);

            // Unused states
            //config.configure(Type.INITIALIZED);
            //config.configure(Type.INVALID_CONFIGURATION);
            //config.configure(IOState.Type.FAILED);

            config.configure(IOState.Type.STARTING)
                    .permit(IOState.Trigger.RUNNING, IOState.Type.RUNNING)
                    .permit(IOState.Trigger.FAIL, IOState.Type.FAILING)
                    .onEntry(trace::accept)
                    .onEntry(() -> inputLifecycle.start(inputState))
                    .ignore(IOState.Trigger.START)
                    .ignore(IOState.Trigger.STOP)
                    .ignore(IOState.Trigger.STOPPED)
                    .ignore(IOState.Trigger.TERMINATE)
                    .ignore(IOState.Trigger.SETUP);

            config.configure(IOState.Type.RUNNING)
                    .permit(IOState.Trigger.STOP, IOState.Type.STOPPING)
                    .permit(IOState.Trigger.FAIL, IOState.Type.FAILING)
                    .permit(IOState.Trigger.SETUP, IOState.Type.SETUP)
                    .onEntry(trace::accept)
                    .ignore(IOState.Trigger.START)
                    .ignore(IOState.Trigger.RUNNING)
                    .ignore(IOState.Trigger.STOPPED)
                    .ignore(IOState.Trigger.TERMINATE);

            config.configure(IOState.Type.FAILING)
                    .permit(IOState.Trigger.RUNNING, IOState.Type.RUNNING)
                    .permit(IOState.Trigger.STOP, IOState.Type.STOPPING)
                    .permit(IOState.Trigger.SETUP, IOState.Type.SETUP)
                    .onEntry(trace::accept)
                    .onEntryFrom(failTrigger, failure -> {
                        inputState.setDetailedMessage(failure.detailedMessage());

                        LoggerFactory.getLogger(failure.loggingClass())
                                .atLevel(failure.level())
                                .log(failure.message(), failure.exception());
                    })
                    .ignore(IOState.Trigger.START)
                    .ignore(IOState.Trigger.FAIL)
                    .ignore(IOState.Trigger.STOPPED)
                    .ignore(IOState.Trigger.TERMINATE);

            config.configure(IOState.Type.STOPPING)
                    .permit(IOState.Trigger.STOPPED, IOState.Type.STOPPED)
                    .onEntry(trace::accept)
                    .onEntry(() -> inputLifecycle.stop(inputState))
                    .ignore(IOState.Trigger.START)
                    .ignore(IOState.Trigger.RUNNING)
                    .ignore(IOState.Trigger.FAIL)
                    .ignore(IOState.Trigger.STOP)
                    .ignore(IOState.Trigger.TERMINATE)
                    .ignore(IOState.Trigger.SETUP);

            config.configure(IOState.Type.STOPPED)
                    .permit(IOState.Trigger.TERMINATE, IOState.Type.TERMINATED)
                    .permit(IOState.Trigger.SETUP, IOState.Type.SETUP)
                    .onEntry(trace::accept)
                    .ignore(IOState.Trigger.START)
                    .ignore(IOState.Trigger.RUNNING)
                    .ignore(IOState.Trigger.FAIL)
                    .ignore(IOState.Trigger.STOP)
                    .ignore(IOState.Trigger.STOPPED);

            config.configure(IOState.Type.TERMINATED)
                    .permit(IOState.Trigger.SETUP, IOState.Type.SETUP)
                    .onEntry(trace::accept)
                    .ignore(IOState.Trigger.START)
                    .ignore(IOState.Trigger.RUNNING)
                    .ignore(IOState.Trigger.FAIL)
                    .ignore(IOState.Trigger.STOP)
                    .ignore(IOState.Trigger.STOPPED)
                    .ignore(IOState.Trigger.TERMINATE);

            return new MessageInputStateMachine(initialState, config, failTrigger);
        }
    }
}
