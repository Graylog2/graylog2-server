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
package org.graylog2.plugin;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.plugin.events.inputs.IOStateChangedEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InputFailureRecorderTest {

    private final EventBus eventBus = new EventBus();
    private final StateChangeCollector stateChanges = new StateChangeCollector();
    private final IOState<MessageInput> inputState =
            new IOState<>(eventBus, mock(MessageInput.class), IOState.Type.RUNNING);
    private final InputFailureRecorder recorder = new InputFailureRecorder(inputState);

    @BeforeEach
    void registerCollector() {
        eventBus.register(stateChanges);
    }

    @Test
    void setFailingKeepsTheMessageOfAFailureAlreadyRecorded() {
        recorder.setFailing(getClass(), "first");
        recorder.setFailing(getClass(), "second");

        assertThat(inputState.getState()).isEqualTo(IOState.Type.FAILING);
        assertThat(inputState.getDetailedMessage()).isEqualTo("first");
    }

    /**
     * A terminal failure carries the only actionable detail - the denied action and resource - so it has to survive an
     * earlier transient failure rather than be dropped by the de-duplication in {@code setFailing}.
     */
    @Test
    void setTerminallyFailingReplacesTheMessageOfAFailureAlreadyRecorded() {
        recorder.setFailing(getClass(), "Errors for Kinesis stream <test-stream>!");

        recorder.setTerminallyFailing(getClass(), "AWS authorization failure",
                new RuntimeException("not authorized to perform: dynamodb:Query"));

        assertThat(inputState.getState()).isEqualTo(IOState.Type.FAILING);
        assertThat(inputState.getDetailedMessage())
                .isEqualTo("AWS authorization failure: (not authorized to perform: dynamodb:Query)");
    }

    /**
     * The notification, the system message and the persisted runtime state are all written by
     * {@link IOStateChangedEvent} subscribers, so a replacement message that publishes no event stays invisible
     * everywhere except to callers reading the input state directly.
     */
    @Test
    void publishesAnEventWhenATerminalMessageReplacesATransientOne() {
        recorder.setFailing(getClass(), "Errors for Kinesis stream <test-stream>!");
        assertThat(stateChanges.events).hasSize(1);

        recorder.setTerminallyFailing(getClass(), "AWS authorization failure", null);

        assertThat(stateChanges.events).hasSize(2);
        assertThat(stateChanges.events.get(1).changedState().getDetailedMessage())
                .isEqualTo("AWS authorization failure");
    }

    @Test
    void publishesNoEventWhenNeitherStateNorMessageChanges() {
        recorder.setFailing(getClass(), "same");
        recorder.setFailing(getClass(), "same");

        assertThat(stateChanges.events).hasSize(1);
    }

    @Test
    void setTerminallyFailingMovesARunningInputToFailing() {
        recorder.setTerminallyFailing(getClass(), "AWS authorization failure", null);

        assertThat(inputState.getState()).isEqualTo(IOState.Type.FAILING);
        assertThat(inputState.getDetailedMessage()).isEqualTo("AWS authorization failure");
    }

    /**
     * Work that continues after a failure retrying cannot resolve - KCL keeps draining the leases it already holds -
     * must not report the input healthy again, or it ends up displaying RUNNING with nothing behind it.
     */
    @Test
    void setRunningDoesNotClearATerminalFailure() {
        recorder.setTerminallyFailing(getClass(), "AWS authorization failure", null);

        recorder.setRunning();

        assertThat(inputState.getState()).isEqualTo(IOState.Type.FAILING);
        assertThat(inputState.getDetailedMessage()).isEqualTo("AWS authorization failure");
    }

    /**
     * The at-most-once property is the caller's to enforce, but the recorder must not turn a repeated report into a
     * stream of state writes: every published event costs a system message, a notification rebuild and a Mongo upsert.
     */
    @Test
    void setTerminallyFailingKeepsTheFirstTerminalMessage() {
        recorder.setTerminallyFailing(getClass(), "first terminal failure", null);

        recorder.setTerminallyFailing(getClass(), "second terminal failure", null);

        assertThat(inputState.getDetailedMessage()).isEqualTo("first terminal failure");
        assertThat(stateChanges.events).hasSize(1);
    }

    @Test
    void setFailingDoesNotOverwriteATerminalFailure() {
        recorder.setTerminallyFailing(getClass(), "AWS authorization failure", null);

        recorder.setFailing(getClass(), "Errors for Kinesis stream <test-stream>!");

        assertThat(inputState.getDetailedMessage()).isEqualTo("AWS authorization failure");
    }

    private static class StateChangeCollector {
        private final List<IOStateChangedEvent<MessageInput>> events = new ArrayList<>();

        @Subscribe
        @SuppressWarnings("unused")
        public void onStateChange(IOStateChangedEvent<MessageInput> event) {
            events.add(event);
        }
    }
}
