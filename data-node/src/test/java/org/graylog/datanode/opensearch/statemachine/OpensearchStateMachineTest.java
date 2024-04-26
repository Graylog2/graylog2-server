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
package org.graylog.datanode.opensearch.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpensearchStateMachineTest {

    @Mock
    OpensearchProcess opensearchProcess;

    @Test
    void testOptimalScenario() {
        final StateMachine<OpensearchState, OpensearchEvent> machine = OpensearchStateMachine.createNew(opensearchProcess);
        Assertions.assertEquals(machine.getState(), OpensearchState.WAITING_FOR_CONFIGURATION);

        machine.fire(OpensearchEvent.PROCESS_PREPARED);
        Assertions.assertEquals(OpensearchState.PREPARED, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_STARTED);
        Assertions.assertEquals(OpensearchState.STARTING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(OpensearchState.AVAILABLE, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_TERMINATED);
        Assertions.assertEquals(OpensearchState.TERMINATED, machine.getState());
    }

    @Test
    void testRestFailing() {
        final StateMachine<OpensearchState, OpensearchEvent> machine = OpensearchStateMachine.createNew(opensearchProcess);
        Assertions.assertEquals(machine.getState(), OpensearchState.WAITING_FOR_CONFIGURATION);

        machine.fire(OpensearchEvent.PROCESS_PREPARED);
        Assertions.assertEquals(OpensearchState.PREPARED, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_STARTED);
        Assertions.assertEquals(OpensearchState.STARTING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(OpensearchState.AVAILABLE, machine.getState());
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);

        // three failures are still accepted
        Assertions.assertEquals(OpensearchState.NOT_RESPONDING, machine.getState());

        // fourth should bring the state to FAILED
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(OpensearchState.FAILED, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(OpensearchState.AVAILABLE, machine.getState());
    }

    @Test
    void testStartupFailure() {
        final StateMachine<OpensearchState, OpensearchEvent> machine = OpensearchStateMachine.createNew(opensearchProcess);
        Assertions.assertEquals(machine.getState(), OpensearchState.WAITING_FOR_CONFIGURATION);

        machine.fire(OpensearchEvent.PROCESS_PREPARED);
        Assertions.assertEquals(OpensearchState.PREPARED, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_STARTED);
        Assertions.assertEquals(OpensearchState.STARTING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(OpensearchState.STARTING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        // after five repetitions we give up waiting for the process start and fail
        Assertions.assertEquals(OpensearchState.FAILED, machine.getState());
    }

    @Test
    void testStartupFailureResolved() {
        final StateMachine<OpensearchState, OpensearchEvent> machine = OpensearchStateMachine.createNew(opensearchProcess);
        Assertions.assertEquals(machine.getState(), OpensearchState.WAITING_FOR_CONFIGURATION);

        machine.fire(OpensearchEvent.PROCESS_PREPARED);
        Assertions.assertEquals(OpensearchState.PREPARED, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_STARTED);
        Assertions.assertEquals(OpensearchState.STARTING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(OpensearchState.STARTING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        machine.fire(OpensearchEvent.HEALTH_CHECK_OK);
        // succeeded just in time before we give up
        Assertions.assertEquals(OpensearchState.AVAILABLE, machine.getState());
    }

    @Test
    void testSuccessfullRemoval() {
        final StateMachine<OpensearchState, OpensearchEvent> machine = OpensearchStateMachine.createNew(opensearchProcess);
        Assertions.assertEquals(machine.getState(), OpensearchState.WAITING_FOR_CONFIGURATION);

        machine.fire(OpensearchEvent.PROCESS_PREPARED);
        Assertions.assertEquals(OpensearchState.PREPARED, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_STARTED);
        Assertions.assertEquals(OpensearchState.STARTING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(OpensearchState.AVAILABLE, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_REMOVE);
        Assertions.assertEquals(OpensearchState.REMOVING, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_STOPPED);
        Assertions.assertEquals(OpensearchState.REMOVED, machine.getState());

    }

    @Test
    void testFailingRemoval() {
        final StateMachine<OpensearchState, OpensearchEvent> machine = OpensearchStateMachine.createNew(opensearchProcess);
        Assertions.assertEquals(machine.getState(), OpensearchState.WAITING_FOR_CONFIGURATION);

        machine.fire(OpensearchEvent.PROCESS_PREPARED);
        Assertions.assertEquals(OpensearchState.PREPARED, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_STARTED);
        Assertions.assertEquals(OpensearchState.STARTING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(OpensearchState.AVAILABLE, machine.getState());

        machine.fire(OpensearchEvent.PROCESS_REMOVE);
        Assertions.assertEquals(OpensearchState.REMOVING, machine.getState());

        machine.fire(OpensearchEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(OpensearchState.FAILED, machine.getState());

    }

}
