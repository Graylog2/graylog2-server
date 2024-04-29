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
package org.graylog.datanode.opensearch.statemachine.tracer;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpensearchWatchdogTracerTest {

    @Mock
    OpensearchProcess opensearchProcess;

    @Test
    void testLifecycle() {
        OpensearchStateMachine stateMachine = OpensearchStateMachine.createNew(opensearchProcess);
        final OpensearchWatchdog watchdog = new OpensearchWatchdog(stateMachine, 3);
        stateMachine.getTracerAggregator().addTracer(watchdog);
        stateMachine.fire(OpensearchEvent.PROCESS_STARTED);

        // both process and watchdog are running now. Let's stop the process and see if the watchdog will restart it
        terminateProcess(stateMachine);

        // see if the process is starting again
        Assertions.assertThat(isInStartingState(stateMachine)).isTrue();

        // repeat
        terminateProcess(stateMachine);
        Assertions.assertThat(isInStartingState(stateMachine)).isTrue();

        terminateProcess(stateMachine);
        Assertions.assertThat(isInStartingState(stateMachine)).isTrue();

        // this is the 4th termination, we give up trying
        terminateProcess(stateMachine);

        Assertions.assertThat(watchdog.isActive()).isFalse();
        Assertions.assertThat(stateMachine.getState()).isEqualTo(OpensearchState.TERMINATED);
    }

    private void terminateProcess(OpensearchStateMachine stateMachine) {
        stateMachine.fire(OpensearchEvent.PROCESS_TERMINATED);
    }

    private boolean isInStartingState(OpensearchStateMachine stateMachine) {
        return stateMachine.getState() == OpensearchState.STARTING;
    }
}
